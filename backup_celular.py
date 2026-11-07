"""Copia fotos de um Motorola conectado por ADB e registra a cópia no Supabase."""
from __future__ import annotations

import json
import os
import subprocess
import sys
import urllib.error
import urllib.request
from datetime import datetime, timezone
from pathlib import Path

ROOT = Path(__file__).resolve().parent
BACKUP_DIR = ROOT / "backup"


def load_env_file() -> None:
    """Carrega valores de .env.local sem imprimir nenhum deles."""
    path = ROOT / ".env.local"
    if not path.exists():
        return
    for raw in path.read_text(encoding="utf-8").splitlines():
        line = raw.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        name, value = line.split("=", 1)
        os.environ.setdefault(name.strip(), value.strip().strip('"').strip("'"))


def run_adb(*args: str) -> subprocess.CompletedProcess[str]:
    return subprocess.run(["adb", *args], check=True, text=True, capture_output=True)


def register_backup(device: str, source: str, destination: Path) -> None:
    url = os.environ.get("NEXT_PUBLIC_SUPABASE_URL", "").rstrip("/")
    key = os.environ.get("SUPABASE_SERVICE_ROLE_KEY", "")
    if not url or not key:
        print("Cópia concluída; configure o acesso privado do Supabase para registrar os arquivos.")
        return
    payload = [{"device": device, "source_path": source, "backup_path": str(destination),
                "created_at": datetime.now(timezone.utc).isoformat()}]
    request = urllib.request.Request(
        f"{url}/rest/v1/mobile_backups", data=json.dumps(payload).encode(),
        headers={"apikey": key, "Authorization": f"Bearer {key}",
                 "Content-Type": "application/json", "Prefer": "return=minimal"}, method="POST")
    try:
        with urllib.request.urlopen(request, timeout=20):
            pass
    except (urllib.error.URLError, urllib.error.HTTPError) as error:
        raise RuntimeError("A cópia foi feita, mas o registro no Supabase falhou.") from error


def main() -> int:
    load_env_file()
    try:
        devices = [line.split("\t")[0] for line in run_adb("devices").stdout.splitlines()[1:]
                   if "\tdevice" in line]
        if not devices:
            print("Nenhum celular autorizado encontrado. Conecte o Motorola e aceite a autorização USB.", file=sys.stderr)
            return 1
        if len(devices) > 1:
            print("Há mais de um aparelho conectado; deixe apenas o Motorola conectado.", file=sys.stderr)
            return 1
        device = devices[0]
        model = run_adb("-s", device, "shell", "getprop", "ro.product.model").stdout.strip() or "Motorola"
        BACKUP_DIR.mkdir(parents=True, exist_ok=True)
        copied = 0
        for source in ("/sdcard/DCIM/Camera", "/sdcard/Pictures"):
            target = BACKUP_DIR / device / source.strip("/").replace("/", "_")
            result = subprocess.run(["adb", "-s", device, "pull", source, str(target)],
                                    text=True, capture_output=True)
            if result.returncode == 0:
                register_backup(model, source, target)
                copied += 1
            elif "does not exist" not in result.stderr.lower():
                raise RuntimeError("O ADB não conseguiu copiar uma das pastas de fotos.")
        print(f"Cópia concluída em {BACKUP_DIR}; pastas copiadas: {copied}.")
        return 0
    except FileNotFoundError:
        print("ADB não encontrado. Instale o Android Platform Tools e tente novamente.", file=sys.stderr)
        return 1
    except (subprocess.CalledProcessError, RuntimeError) as error:
        print(str(error) or "Falha ao copiar as fotos.", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
