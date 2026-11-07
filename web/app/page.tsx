"use client";

import { FormEvent, useState } from "react";

type Message = { role: "user" | "assistant"; content: string };

export default function Home() {
  const [messages, setMessages] = useState<Message[]>([]);
  const [question, setQuestion] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");

  async function send(event: FormEvent) {
    event.preventDefault();
    const text = question.trim();
    if (!text || busy) return;
    setMessages((items) => [...items, { role: "user", content: text }]);
    setQuestion(""); setBusy(true); setError("");
    try {
      const response = await fetch("/api/chat", { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ question: text }) });
      const result = await response.json();
      if (!response.ok) throw new Error(result.error || "Não foi possível responder agora.");
      setMessages((items) => [...items, { role: "assistant", content: result.answer }]);
    } catch (cause) { setError(cause instanceof Error ? cause.message : "Ocorreu um erro."); }
    finally { setBusy(false); }
  }

  return <main><h1>Chat Midiastory</h1><p className="hint">Faça uma pergunta sobre seus documentos.</p><section className="chat" aria-live="polite">
    {messages.map((message, i) => <div className={`message ${message.role}`} key={i}>{message.content}</div>)}
    {busy && <p>Preparando resposta…</p>}{error && <p className="error">{error}</p>}
  </section><form onSubmit={send}><textarea value={question} onChange={(e) => setQuestion(e.target.value)} placeholder="Escreva sua pergunta…" aria-label="Sua pergunta" /><button disabled={busy || !question.trim()}>{busy ? "Aguarde…" : "Enviar"}</button></form></main>;
}
