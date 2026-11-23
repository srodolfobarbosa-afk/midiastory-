param(
  [string]$AvatarPath = "C:\Users\rodolfo\midiastory-\.agents\avatar\antigravity-avatar.png",
  [string]$FfmpegPath = "C:\Users\rodolfo\tools\ffmpeg\bin\ffmpeg.exe",
  [string]$FfplayPath = "C:\Users\rodolfo\tools\ffmpeg\bin\ffplay.exe",
  [string]$ScrcpyPath = "C:\Users\rodolfo\tools\scrcpy\scrcpy.exe",
  [string]$NodePath = "C:\Program Files\nodejs\node.exe",
  [string]$TranscribeScript = "C:\Users\rodolfo\midiastory-\scripts\voice-transcribe.mjs",
  [string]$BoardScript = "C:\Users\rodolfo\midiastory-\scripts\antigravity-board.mjs",
  [string]$VisemeAnalyzerScript = "C:\Users\rodolfo\midiastory-\scripts\analyze-wav-visemes.mjs",
  [string]$MicDevice = "Microphone (High Definition Audio Device)"
)

Add-Type -AssemblyName PresentationFramework
Add-Type -AssemblyName PresentationCore
Add-Type -AssemblyName WindowsBase
Add-Type -AssemblyName System.Windows.Forms
Add-Type -AssemblyName System.Drawing

# Win32 helper to focus the Antigravity console window and send Unicode text instantly via WM_CHAR (500x faster than SendKeys!)
Add-Type @"
using System;
using System.Runtime.InteropServices;
public class WinFocusHelper {
  [DllImport("user32.dll")]
  public static extern bool SetForegroundWindow(IntPtr hWnd);
  [DllImport("user32.dll")]
  public static extern bool BringWindowToTop(IntPtr hWnd);
  [DllImport("user32.dll")]
  public static extern IntPtr GetForegroundWindow();
  [DllImport("user32.dll")]
  public static extern uint GetWindowThreadProcessId(IntPtr hWnd, out uint lpdwProcessId);
  [DllImport("user32.dll")]
  public static extern bool AttachThreadInput(uint idAttach, uint idAttachTo, bool fAttach);
  [DllImport("kernel32.dll")]
  public static extern uint GetCurrentThreadId();
  [DllImport("user32.dll")]
  public static extern bool ShowWindow(IntPtr hWnd, int nCmdShow);
  [DllImport("user32.dll", CharSet = CharSet.Unicode)]
  public static extern bool PostMessageW(IntPtr hWnd, uint Msg, IntPtr wParam, IntPtr lParam);

  public const uint WM_CHAR = 0x0102;
  public const uint WM_KEYDOWN = 0x0100;
  public const uint WM_KEYUP = 0x0101;
  public const int VK_RETURN = 0x0D;

  public static void ForceFocus(IntPtr hWnd) {
    IntPtr fg = GetForegroundWindow();
    uint pidDummy = 0;
    uint fgThread = fg != IntPtr.Zero ? GetWindowThreadProcessId(fg, out pidDummy) : 0;
    uint myThread = GetCurrentThreadId();
    bool attached = false;
    if (fgThread != 0 && fgThread != myThread) {
      attached = AttachThreadInput(myThread, fgThread, true);
    }
    ShowWindow(hWnd, 9);
    BringWindowToTop(hWnd);
    SetForegroundWindow(hWnd);
    if (attached) {
      AttachThreadInput(myThread, fgThread, false);
    }
  }

  public static void SendTextFast(IntPtr hWnd, string text) {
    int i = 0;
    foreach (char c in text) {
      if (c == '\r' || c == '\n') {
        PostMessageW(hWnd, WM_CHAR, (IntPtr)' ', IntPtr.Zero);
      } else {
        PostMessageW(hWnd, WM_CHAR, (IntPtr)c, IntPtr.Zero);
      }
      i++;
      if ((i % 25) == 0) System.Threading.Thread.Sleep(4);
    }
    System.Threading.Thread.Sleep(200);
    PostMessageW(hWnd, WM_KEYDOWN, (IntPtr)VK_RETURN, IntPtr.Zero);
    PostMessageW(hWnd, WM_KEYUP, (IntPtr)VK_RETURN, IntPtr.Zero);
  }
}
"@

# Single-instance guard via mutex
$createdNew = $false
$mutex = New-Object System.Threading.Mutex($true, "Global\AntigravityVoiceCompanionMutex", [ref]$createdNew)
if (-not $createdNew) {
  exit 0
}

$queueDir = Join-Path $env:TEMP "antigravity-voice-queue"
$lastSpokenWav = Join-Path $env:TEMP "antigravity-last-spoken.wav"
$heartbeatPath = Join-Path $env:TEMP "antigravity-companion-heartbeat.txt"
$configPath = Join-Path $env:TEMP "midiastory-voice-config.json"
New-Item -ItemType Directory -Force -Path $queueDir | Out-Null

$workArea = [System.Windows.SystemParameters]::WorkArea
$normalW = 224
$normalH = 330
$maxW = 350
$maxH = 460
$minW = 224
$minH = 48

$leftPos = [Math]::Max(16, [int]($workArea.Right - $normalW - 20))
$topPos = [Math]::Max(16, [int]($workArea.Bottom - $normalH - 20))

$xaml = @"
<Window xmlns="http://schemas.microsoft.com/winfx/2006/xaml/presentation"
        xmlns:x="http://schemas.microsoft.com/winfx/2006/xaml"
        Title="Antigravity Voz"
        WindowStyle="None"
        AllowsTransparency="True"
        Background="Transparent"
        Topmost="True"
        ShowInTaskbar="True"
        ShowActivated="False"
        ResizeMode="NoResize"
        Width="$normalW"
        Height="$normalH"
        Left="$leftPos"
        Top="$topPos">
  <Border x:Name="OuterBorder" CornerRadius="20" Background="#F20B0B0F" BorderBrush="#0A84FF" BorderThickness="2" Margin="4">
    <Grid Margin="10,8,10,10">
      <Grid.RowDefinitions>
        <RowDefinition Height="Auto"/>
        <RowDefinition Height="*"/>
        <RowDefinition Height="Auto"/>
      </Grid.RowDefinitions>

      <!-- Header Bar (Draggable + Minimize / Maximize / Close) -->
      <Grid x:Name="HeaderBar" Grid.Row="0" Background="Transparent" Cursor="SizeAll">
        <Grid.ColumnDefinitions>
          <ColumnDefinition Width="*"/>
          <ColumnDefinition Width="Auto"/>
        </Grid.ColumnDefinitions>
        <StackPanel Grid.Column="0" Orientation="Horizontal" VerticalAlignment="Center">
          <Ellipse x:Name="StatusDot" Width="8" Height="8" Fill="#0A84FF" Margin="2,0,6,0"/>
          <TextBlock x:Name="TitleText" Text="Antigravity" Foreground="#F2F2F7" FontFamily="Segoe UI" FontSize="12" FontWeight="SemiBold" VerticalAlignment="Center"/>
        </StackPanel>
        <StackPanel Grid.Column="1" Orientation="Horizontal">
          <Button x:Name="BtnMin" Content="—" Width="24" Height="22" Margin="2,0,0,0" Background="#1C1C1E" Foreground="#E5E5EA" BorderThickness="0" FontSize="11" Cursor="Hand" ToolTip="Minimizar / Restaurar"/>
          <Button x:Name="BtnMax" Content="□" Width="24" Height="22" Margin="3,0,0,0" Background="#1C1C1E" Foreground="#E5E5EA" BorderThickness="0" FontSize="11" Cursor="Hand" ToolTip="Maximizar / Tamanho normal"/>
          <Button x:Name="BtnClose" Content="✕" Width="24" Height="22" Margin="3,0,0,0" Background="#1C1C1E" Foreground="#FF453A" BorderThickness="0" FontSize="11" Cursor="Hand" ToolTip="Fechar"/>
        </StackPanel>
      </Grid>

      <!-- Body: Living Animated Avatar + Status -->
      <Grid x:Name="BodyPanel" Grid.Row="1" Margin="0,8,0,8">
        <Grid.RowDefinitions>
          <RowDefinition Height="*"/>
          <RowDefinition Height="Auto"/>
        </Grid.RowDefinitions>
        <Border x:Name="AvatarFrame" Grid.Row="0" Width="140" Height="140" CornerRadius="70"
                BorderBrush="#0A84FF" BorderThickness="2.5" Background="#1C1C1E"
                HorizontalAlignment="Center" VerticalAlignment="Center" Cursor="Hand"
                RenderTransformOrigin="0.5,0.5"
                ToolTip="Clique para arrastar ou silenciar">
          <Border.RenderTransform>
            <TransformGroup>
              <ScaleTransform x:Name="AvatarScale" ScaleX="1" ScaleY="1"/>
              <RotateTransform x:Name="AvatarRotate" Angle="0"/>
              <TranslateTransform x:Name="AvatarTranslate" X="0" Y="0"/>
            </TransformGroup>
          </Border.RenderTransform>
          <Grid>
            <Ellipse x:Name="AvatarEllipse" Width="134" Height="134"/>
            <Ellipse x:Name="AvatarOverlayEllipse" Width="134" Height="134" Opacity="0"/>
          </Grid>
        </Border>
        <TextBlock x:Name="StatusText" Grid.Row="1" Text="Pronta para ouvir" Foreground="#AEAEB2"
                   FontFamily="Segoe UI" FontSize="11.5" HorizontalAlignment="Center" Margin="0,8,0,0" TextTrimming="CharacterEllipsis"/>
      </Grid>

      <!-- Controls: Mic + Voice Toggle + Quick Tools (Motorola, Quadro Professor, Web) -->
      <StackPanel x:Name="ControlsPanel" Grid.Row="2">
        <Grid Margin="0,0,0,6">
          <Grid.ColumnDefinitions>
            <ColumnDefinition Width="*"/>
            <ColumnDefinition Width="Auto"/>
            <ColumnDefinition Width="Auto"/>
          </Grid.ColumnDefinitions>
          <Button x:Name="BtnMic" Grid.Column="0" Content="🎤 Falar" Height="34" Margin="0,0,5,0"
                  Background="#0A84FF" Foreground="#FFFFFF" BorderThickness="0"
                  FontFamily="Segoe UI" FontSize="12.5" FontWeight="SemiBold" Cursor="Hand"
                  ToolTip="Clique para falar; clique novamente para enviar no chat"/>
          <Button x:Name="BtnReplay" Grid.Column="1" Content="🔁" Width="34" Height="34" Margin="0,0,5,0"
                  Background="#1C1C1E" Foreground="#64D2FF" BorderThickness="0"
                  FontSize="14" Cursor="Hand" ToolTip="Repetir a última fala da IA (custo zero)"/>
          <Button x:Name="BtnMute" Grid.Column="2" Content="🔊" Width="34" Height="34"
                  Background="#1C1C1E" Foreground="#F2F2F7" BorderThickness="0"
                  FontSize="14" Cursor="Hand" ToolTip="Ligar/Desligar voz da IA"/>
        </Grid>
        <UniformGrid Columns="4" Height="28">
          <Button x:Name="BtnMoto" Content="📱Moto" Margin="0,0,2,0" Background="#1C1C1E" Foreground="#E5E5EA" BorderThickness="0" FontFamily="Segoe UI" FontSize="10.5" Cursor="Hand" ToolTip="Espelhar tela do Motorola USB no PC"/>
          <Button x:Name="BtnBoard" Content="📊HUD" Margin="1,0,1,0" Background="#1C1C1E" Foreground="#30D158" BorderThickness="0" FontFamily="Segoe UI" FontSize="10.5" Cursor="Hand" ToolTip="Atualizar e abrir Quadro FRIDAY Mission HUD ao vivo"/>
          <Button x:Name="BtnScreen" Content="👁️Tela" Margin="1,0,1,0" Background="#1C1C1E" Foreground="#64D2FF" BorderThickness="0" FontFamily="Segoe UI" FontSize="10.5" Cursor="Hand" ToolTip="Capturar sua tela agora para a IA enxergar e te orientar"/>
          <Button x:Name="BtnWeb" Content="🌐Web" Margin="2,0,0,0" Background="#1C1C1E" Foreground="#E5E5EA" BorderThickness="0" FontFamily="Segoe UI" FontSize="10.5" Cursor="Hand" ToolTip="Abrir site oficial Midiastore"/>
        </UniformGrid>
      </StackPanel>
    </Grid>
  </Border>
</Window>
"@

$reader = New-Object System.Xml.XmlNodeReader ([xml]$xaml)
$window = [Windows.Markup.XamlReader]::Load($reader)

$outerBorder = $window.FindName("OuterBorder")
$headerBar = $window.FindName("HeaderBar")
$bodyPanel = $window.FindName("BodyPanel")
$controlsPanel = $window.FindName("ControlsPanel")
$avatarFrame = $window.FindName("AvatarFrame")
$avatarEllipse = $window.FindName("AvatarEllipse")
$avatarOverlayEllipse = $window.FindName("AvatarOverlayEllipse")
$statusDot = $window.FindName("StatusDot")
$statusText = $window.FindName("StatusText")
$btnMin = $window.FindName("BtnMin")
$btnMax = $window.FindName("BtnMax")
$btnClose = $window.FindName("BtnClose")
$btnMic = $window.FindName("BtnMic")
$btnReplay = $window.FindName("BtnReplay")
$btnMute = $window.FindName("BtnMute")
$btnMoto = $window.FindName("BtnMoto")
$btnBoard = $window.FindName("BtnBoard")
$btnScreen = $window.FindName("BtnScreen")
$btnWeb = $window.FindName("BtnWeb")
$avatarScale = $window.FindName("AvatarScale")
$avatarRotate = $window.FindName("AvatarRotate")
$avatarTranslate = $window.FindName("AvatarTranslate")

# Load avatar animation frames (idle, blink, talk-mid, talk-open)
function Load-AvatarBrush([string]$imgPath, $fallbackBrush) {
  if ($imgPath -and (Test-Path -LiteralPath $imgPath)) {
    try {
      $bmp = New-Object System.Windows.Media.Imaging.BitmapImage
      $bmp.BeginInit()
      $bmp.CacheOption = [System.Windows.Media.Imaging.BitmapCacheOption]::OnLoad
      $bmp.UriSource = New-Object System.Uri($imgPath)
      $bmp.EndInit()
      $bmp.Freeze()
      $br = New-Object System.Windows.Media.ImageBrush
      $br.ImageSource = $bmp
      $br.Stretch = [System.Windows.Media.Stretch]::UniformToFill
      return $br
    } catch {}
  }
  return $fallbackBrush
}

$avatarDir = Split-Path -Parent $AvatarPath
$defaultFallback = [System.Windows.Media.Brushes]::DarkSlateBlue
$script:brushIdle = Load-AvatarBrush $AvatarPath $defaultFallback
$script:brushBlink = Load-AvatarBrush (Join-Path $avatarDir "avatar-blink.png") $script:brushIdle
$script:brushTalkMid = Load-AvatarBrush (Join-Path $avatarDir "avatar-talk-mid.png") $script:brushIdle
$script:brushTalkOpen = Load-AvatarBrush (Join-Path $avatarDir "avatar-talk-open.png") $script:brushTalkMid
$script:brushGestureExplain = Load-AvatarBrush (Join-Path $avatarDir "avatar-gesture-explain.png") $script:brushTalkOpen
$script:brushListenTilt = Load-AvatarBrush (Join-Path $avatarDir "avatar-listen-tilt.png") $script:brushIdle
$script:brushWave = Load-AvatarBrush (Join-Path $avatarDir "avatar-wave.png") $script:brushGestureExplain
$script:brushKiss = Load-AvatarBrush (Join-Path $avatarDir "avatar-kiss.png") $script:brushIdle
$script:brushSmile = Load-AvatarBrush (Join-Path $avatarDir "avatar-smile.png") $script:brushIdle

# Load 16 continuous Optical-Flow mouth levels (lip_01..lip_16) and 6 eyelid blink levels (blink_01..blink_06) for 30 FPS video-smooth speech!
$script:lipFlowBrushes = New-Object System.Collections.Generic.List[System.Windows.Media.Brush]
$script:blinkFlowBrushes = New-Object System.Collections.Generic.List[System.Windows.Media.Brush]
$lipFlowDir = Join-Path $avatarDir "lip-flow"
if (Test-Path -LiteralPath $lipFlowDir) {
  $lFiles = Get-ChildItem -LiteralPath $lipFlowDir -Filter "lip_*.jpg" -ErrorAction SilentlyContinue | Sort-Object Name
  foreach ($lf in $lFiles) {
    $b = Load-AvatarBrush $lf.FullName $null
    if ($b) { $script:lipFlowBrushes.Add($b) }
  }
  $bFiles = Get-ChildItem -LiteralPath $lipFlowDir -Filter "blink_*.jpg" -ErrorAction SilentlyContinue | Sort-Object Name
  foreach ($bf in $bFiles) {
    $b = Load-AvatarBrush $bf.FullName $null
    if ($b) { $script:blinkFlowBrushes.Add($b) }
  }
}
$script:smoothMouth = 0.0
$script:blinkSeq = @(0, 1, 2, 3, 4, 5, 5, 4, 3, 2, 1, 0)
$script:blinkTick = -1
$script:lipSyncFps = 60

$avatarEllipse.Fill = if ($script:lipFlowBrushes.Count -gt 0) { $script:lipFlowBrushes[0] } else { $script:brushIdle }
$script:activeBrush = $script:brushIdle
$script:targetBrush = $script:brushIdle
$script:blendAlpha = 1.0
$script:gestureShowStartMs = -99999

function Set-AvatarPoseSmooth($newBrush, [double]$step = 0.34) {
  if (-not $newBrush) { return }
  if ($newBrush -ne $script:targetBrush) {
    if ($script:blendAlpha -ge 0.95) {
      $avatarEllipse.Fill = $script:targetBrush
      $script:activeBrush = $script:targetBrush
    }
    $script:targetBrush = $newBrush
    $avatarOverlayEllipse.Fill = $newBrush
    $script:blendAlpha = 0.0
  }
  if ($script:blendAlpha -lt 1.0) {
    $script:blendAlpha = [Math]::Min(1.0, $script:blendAlpha + $step)
    $avatarOverlayEllipse.Opacity = $script:blendAlpha
    if ($script:blendAlpha -ge 1.0) {
      $avatarEllipse.Fill = $script:targetBrush
      $script:activeBrush = $script:targetBrush
      $avatarOverlayEllipse.Opacity = 0.0
    }
  }
}

# Compute 60 FPS Phonetic-Viseme (F1/F2 + ZCR + strict silence gate) track via Node.js (zero Python)
# Returns int[] with values 0..15 mapped directly to lip_01..lip_16 (0 = strict silence / closed lips)
function Get-WavLipSyncEnvelope([string]$wavFile, [int]$fps = 60) {
  if ((Test-Path -LiteralPath $NodePath) -and (Test-Path -LiteralPath $VisemeAnalyzerScript)) {
    try {
      $psi = New-Object System.Diagnostics.ProcessStartInfo
      $psi.FileName = $NodePath
      $psi.Arguments = "`"$VisemeAnalyzerScript`" `"$wavFile`""
      $psi.UseShellExecute = $false
      $psi.RedirectStandardOutput = $true
      $psi.CreateNoWindow = $true
      $p = [System.Diagnostics.Process]::Start($psi)
      $hex = $p.StandardOutput.ReadToEnd().Trim()
      if ($p.WaitForExit(4500) -and $p.ExitCode -eq 0 -and $hex.Length -gt 0) {
        $arr = New-Object int[] $hex.Length
        for ($i = 0; $i -lt $hex.Length; $i++) {
          $arr[$i] = [Convert]::ToInt32($hex.Substring($i, 1), 16)
        }
        return $arr
      }
    } catch {}
  }

  # Fallback rápido em PowerShell (60 FPS com gate de silêncio rígido)
  try {
    $bytes = [System.IO.File]::ReadAllBytes($wavFile)
    if ($bytes.Length -lt 64) { return @() }
    $sampleRate = [BitConverter]::ToInt32($bytes, 24)
    $channels = [BitConverter]::ToInt16($bytes, 22)
    if ($sampleRate -lt 8000 -or $sampleRate -gt 96000) { $sampleRate = 24000 }
    if ($channels -lt 1 -or $channels -gt 2) { $channels = 1 }
    $dataOffset = 44
    for ($pos = 12; $pos -lt [Math]::Min($bytes.Length - 8, 256); $pos++) {
      if ($bytes[$pos] -eq 100 -and $bytes[$pos+1] -eq 97 -and $bytes[$pos+2] -eq 116 -and $bytes[$pos+3] -eq 97) {
        $dataOffset = $pos + 8
        break
      }
    }
    $bytesPerFrame = [Math]::Max(2, [int](($sampleRate * $channels * 2) / $fps))
    $step = [Math]::Max(2, 2 * $channels * 4)
    $levels = New-Object System.Collections.Generic.List[double]
    $peak = 0.001
    for ($off = $dataOffset; $off -lt ($bytes.Length - 2); $off += $bytesPerFrame) {
      $end = [Math]::Min($bytes.Length - 2, $off + $bytesPerFrame)
      $sumSq = 0.0
      $cnt = 0
      for ($i = $off; $i -lt $end; $i += $step) {
        $sample = [BitConverter]::ToInt16($bytes, $i) / 32768.0
        $sumSq += ($sample * $sample)
        $cnt++
      }
      $rms = if ($cnt -gt 0) { [Math]::Sqrt($sumSq / $cnt) } else { 0.0 }
      if ($rms -gt $peak) { $peak = $rms }
      $levels.Add($rms)
    }
    $norm = New-Object int[] $levels.Count
    for ($k = 0; $k -lt $levels.Count; $k++) {
      $r = $levels[$k] / $peak
      # Gate de silêncio rígido (< 8% fecha imediatamente para 0)
      $norm[$k] = if ($r -lt 0.08) { 0 } else { [Math]::Min(15, [int][Math]::Round([Math]::Pow($r, 0.78) * 15)) }
    }
    return $norm
  } catch {
    return @()
  }
}

# State variables
$script:viewMode = "normal" # normal | max | min
$script:voiceEnabled = $true
$script:playProc = $null
$script:currentWav = $null
$script:lipSyncFrames = @()
$script:playWatch = New-Object System.Diagnostics.Stopwatch
$script:rng = New-Object System.Random
$script:nextBlinkAt = [DateTime]::UtcNow.AddMilliseconds(2800)
$script:blinkEndAt = [DateTime]::MinValue
$script:recProc = $null
$script:recAudioPath = $null
$script:sttProc = $null
$script:sttOutTxt = $null
$script:sttStartTime = $null
$script:isRecording = $false
$script:pulseTick = 0

function Update-VoiceConfig([bool]$enabled) {
  $script:voiceEnabled = $enabled
  $btnMute.Content = if ($enabled) { "🔊" } else { "🔇" }
  $btnMute.Foreground = if ($enabled) { [System.Windows.Media.Brushes]::White } else { [System.Windows.Media.Brushes]::Gray }
  try {
    $cfg = @{ enabled = $enabled; voiceName = "Kore"; maxChars = 750; showAvatar = $true }
    if (Test-Path -LiteralPath $configPath) {
      $existing = Get-Content -LiteralPath $configPath -Raw | ConvertFrom-Json
      if ($existing.voiceName) { $cfg.voiceName = $existing.voiceName }
      if ($existing.maxChars) { $cfg.maxChars = $existing.maxChars }
    }
    $cfg | ConvertTo-Json | Set-Content -LiteralPath $configPath -Encoding UTF8
  } catch {}
}

function Stop-Playback {
  try {
    if ($script:playProc -and -not $script:playProc.HasExited) {
      $script:playProc.Kill()
    }
  } catch {}
  $script:playProc = $null
  if ($script:currentWav -and ($script:currentWav -ne $lastSpokenWav) -and (Test-Path -LiteralPath $script:currentWav)) {
    try { Remove-Item -LiteralPath $script:currentWav -Force -ErrorAction SilentlyContinue } catch {}
  }
  $script:currentWav = $null
}

function Start-WavPlayback([string]$wavFile, [bool]$saveAsLast = $true) {
  if (-not (Test-Path -LiteralPath $wavFile) -or -not (Test-Path -LiteralPath $FfplayPath)) { return }
  Stop-Playback
  if ($saveAsLast -and ($wavFile -ne $lastSpokenWav)) {
    try { Copy-Item -LiteralPath $wavFile -Destination $lastSpokenWav -Force -ErrorAction SilentlyContinue } catch {}
  }
  $script:currentWav = $wavFile
  $script:lipSyncFrames = Get-WavLipSyncEnvelope $wavFile $script:lipSyncFps
  $psi = New-Object System.Diagnostics.ProcessStartInfo
  $psi.FileName = $FfplayPath
  $psi.Arguments = "-nodisp -autoexit -loglevel quiet `"$wavFile`""
  $psi.UseShellExecute = $false
  $psi.CreateNoWindow = $true
  $script:playProc = [System.Diagnostics.Process]::Start($psi)
  $script:playWatch.Restart()
  $statusText.Text = "Falando..."
}

function Apply-ViewMode([string]$mode) {
  $script:viewMode = $mode
  if ($mode -eq "min") {
    $bodyPanel.Visibility = "Collapsed"
    $controlsPanel.Visibility = "Collapsed"
    $window.Width = $minW
    $window.Height = $minH
    $btnMin.Content = "▲"
  } elseif ($mode -eq "max") {
    $bodyPanel.Visibility = "Visible"
    $controlsPanel.Visibility = "Visible"
    $window.Width = $maxW
    $window.Height = $maxH
    $avatarFrame.Width = 240
    $avatarFrame.Height = 240
    $avatarFrame.CornerRadius = [System.Windows.CornerRadius]::new(120)
    $avatarEllipse.Width = 232
    $avatarEllipse.Height = 232
    $avatarOverlayEllipse.Width = 232
    $avatarOverlayEllipse.Height = 232
    $btnMin.Content = "—"
    $btnMax.Content = "❐"
  } else {
    $bodyPanel.Visibility = "Visible"
    $controlsPanel.Visibility = "Visible"
    $window.Width = $normalW
    $window.Height = $normalH
    $avatarFrame.Width = 136
    $avatarFrame.Height = 136
    $avatarFrame.CornerRadius = [System.Windows.CornerRadius]::new(68)
    $avatarEllipse.Width = 130
    $avatarEllipse.Height = 130
    $avatarOverlayEllipse.Width = 130
    $avatarOverlayEllipse.Height = 130
    $btnMin.Content = "—"
    $btnMax.Content = "□"
  }
  # Keep inside screen bounds
  $wa = [System.Windows.SystemParameters]::WorkArea
  if ($window.Left + $window.Width -gt $wa.Right) { $window.Left = [Math]::Max(8, $wa.Right - $window.Width - 12) }
  if ($window.Top + $window.Height -gt $wa.Bottom) { $window.Top = [Math]::Max(8, $wa.Bottom - $window.Height - 12) }
}

# Draggable header & outer border
$headerBar.Add_MouseLeftButtonDown({
  try { $window.DragMove() } catch {}
})
$avatarFrame.Add_MouseLeftButtonDown({
  if ($script:playProc -and -not $script:playProc.HasExited) {
    Stop-Playback
    $statusText.Text = "Silenciada"
  } else {
    try { $window.DragMove() } catch {}
  }
})

# Window buttons
$btnMin.Add_Click({
  if ($script:viewMode -eq "min") { Apply-ViewMode "normal" } else { Apply-ViewMode "min" }
})
$btnMax.Add_Click({
  if ($script:viewMode -eq "max") { Apply-ViewMode "normal" } else { Apply-ViewMode "max" }
})
$btnClose.Add_Click({
  Stop-Playback
  try { if ($script:recProc -and -not $script:recProc.HasExited) { $script:recProc.Kill() } } catch {}
  try { if (Test-Path -LiteralPath $heartbeatPath) { Remove-Item -LiteralPath $heartbeatPath -Force -ErrorAction SilentlyContinue } } catch {}
  $window.Close()
})
$btnReplay.Add_Click({
  if (Test-Path -LiteralPath $lastSpokenWav) {
    Start-WavPlayback $lastSpokenWav $false
  } else {
    $statusText.Text = "Sem áudio salvo"
  }
})

$btnMute.Add_Click({
  if ($script:voiceEnabled) {
    Stop-Playback
    Update-VoiceConfig $false
    $statusText.Text = "Voz desligada"
  } else {
    Update-VoiceConfig $true
    $statusText.Text = "Voz ligada"
  }
})

# Quick Tool 1: Mirror Motorola USB screen live on PC via scrcpy (with ADB USB check)
$btnMoto.Add_Click({
  $adbExe = "C:\Users\rodolfo\AppData\Local\Android\Sdk\platform-tools\adb.exe"
  $hasDevice = $false
  if (Test-Path -LiteralPath $adbExe) {
    $adbOut = (& $adbExe devices 2>$null) -join "`n"
    if ($adbOut -match "\tdevice\b") { $hasDevice = $true }
  }
  if (-not $hasDevice) {
    $statusText.Text = "Conecte o Moto no USB"
    return
  }
  if (Test-Path -LiteralPath $ScrcpyPath) {
    $statusText.Text = "Espelhando Motorola..."
    $psi = New-Object System.Diagnostics.ProcessStartInfo
    $psi.FileName = $ScrcpyPath
    $psi.Arguments = '--window-title "Motorola Moto G84 — Midiastore" --max-size 900 --stay-awake'
    $psi.UseShellExecute = $false
    $psi.CreateNoWindow = $true
    try {
      [void][System.Diagnostics.Process]::Start($psi)
      Write-DebugLog "Launched scrcpy mirror"
    } catch {
      $statusText.Text = "Erro ao abrir scrcpy"
    }
  } else {
    $statusText.Text = "scrcpy não achado"
  }
})

# Quick Tool 2: Update & Open Live FRIDAY Mission HUD on Professor Board
$btnBoard.Add_Click({
  $statusText.Text = "Atualizando HUD..."
  $hudScript = "C:\Users\rodolfo\midiastory-\scripts\friday-hud.mjs"
  $psi = New-Object System.Diagnostics.ProcessStartInfo
  $psi.FileName = $NodePath
  $psi.Arguments = "`"$hudScript`" --open"
  $psi.UseShellExecute = $false
  $psi.CreateNoWindow = $true
  try { [void][System.Diagnostics.Process]::Start($psi) } catch {}
})

# Quick Tool 3: Instant Screen Vision — capture PC screen so Antigravity can SEE what Rodolfo is looking at!
$btnScreen.Add_Click({
  $statusText.Text = "Capturando tela..."
  try {
    $bounds = [System.Windows.Forms.Screen]::PrimaryScreen.Bounds
    $bmp = New-Object System.Drawing.Bitmap($bounds.Width, $bounds.Height)
    $gfx = [System.Drawing.Graphics]::FromImage($bmp)
    $gfx.CopyFromScreen($bounds.Location, [System.Drawing.Point]::Empty, $bounds.Size)
    $screenPath = Join-Path $env:TEMP "antigravity-live-screen.png"
    $bmp.Save($screenPath, [System.Drawing.Imaging.ImageFormat]::Png)
    $gfx.Dispose()
    $bmp.Dispose()
    $statusText.Text = "Tela enviada para IA!"
    Write-DebugLog "Captured live screen -> $screenPath"
    Send-TextToTerminal "Olhe minha tela atual em $screenPath e me explique ou ajude com o que estou vendo."
  } catch {
    $statusText.Text = "Erro ao capturar tela"
    Write-DebugLog "Screen capture failed: $($_.Exception.Message)"
  }
})

# Quick Tool 4: Open Midiastore Official Web Funnel
$btnWeb.Add_Click({
  $statusText.Text = "Abrindo navegador..."
  try { Start-Process "https://midiastory-df6a0.web.app/" } catch {}
})

$debugLogPath = Join-Path $env:TEMP "antigravity-companion-debug.log"
function Write-DebugLog([string]$msg) {
  try {
    "$(Get-Date -Format 'HH:mm:ss') $msg" | Add-Content -LiteralPath $debugLogPath -Encoding UTF8
  } catch {}
}

function Send-TextToTerminal([string]$text) {
  $clean = $text.Trim()
  if (-not $clean) { return }
  # Find the interactive cmd.exe / WindowsTerminal window (Antigravity CLI)
  $targets = Get-Process -Name "WindowsTerminal","cmd","powershell","pwsh" -ErrorAction SilentlyContinue |
    Where-Object { $_.MainWindowHandle -ne 0 -and $_.Id -ne $PID } |
    Sort-Object { if ($_.MainWindowTitle -match "cmd\.exe|Antigravity|Terminal") { 0 } else { 1 } }, StartTime -Descending
  if ($targets) {
    $target = $targets[0]
    $hWnd = $target.MainWindowHandle
    Write-DebugLog "Focusing PID=$($target.Id) Title='$($target.MainWindowTitle)'"
    [WinFocusHelper]::ForceFocus($hWnd)
    Start-Sleep -Milliseconds 140
    [WinFocusHelper]::SendTextFast($hWnd, $clean)
    Write-DebugLog "Sent Unicode text + ENTER via WM_CHAR in <100ms"
  } else {
    Write-DebugLog "No target terminal window found"
  }
}

# Mic push-to-talk button with Studio Voice DSP (noise suppression + voice boost + ultra-compact MP3)
$btnMic.Add_Click({
  if ($script:sttProc -and -not $script:sttProc.HasExited) { return }

  if (-not $script:isRecording) {
    Stop-Playback
    $script:recAudioPath = Join-Path $env:TEMP ("antigravity-mic-" + [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds() + ".mp3")
    $psi = New-Object System.Diagnostics.ProcessStartInfo
    $psi.FileName = $FfmpegPath
    # Studio DSP pipeline: highpass 100Hz + lowpass 7500Hz + FFT noise reduction + dynamic voice boost -> 32kbps MP3 (~4 KB/sec!)
    $psi.Arguments = "-y -f dshow -audio_buffer_size 50 -i `"audio=$MicDevice`" -t 60 -ar 16000 -ac 1 -af `"highpass=f=100,lowpass=f=7500,afftdn=nf=-25,dynaudnorm=f=150:g=15`" -c:a libmp3lame -b:a 32k `"$($script:recAudioPath)`" -loglevel error"
    $psi.UseShellExecute = $false
    $psi.CreateNoWindow = $true
    $psi.RedirectStandardInput = $true
    try {
      $script:recProc = [System.Diagnostics.Process]::Start($psi)
      $script:isRecording = $true
      $btnMic.Content = "⏹ Enviar"
      $btnMic.Background = (New-Object System.Windows.Media.BrushConverter).ConvertFromString("#FF3B30")
      $outerBorder.BorderBrush = (New-Object System.Windows.Media.BrushConverter).ConvertFromString("#FF3B30")
      $avatarFrame.BorderBrush = (New-Object System.Windows.Media.BrushConverter).ConvertFromString("#FF3B30")
      $statusText.Text = "Ouvindo (DSP ativo)..."
      Write-DebugLog "DSP recording started -> $($script:recAudioPath)"
    } catch {
      $statusText.Text = "Erro ao abrir mic"
      Write-DebugLog "Recording start failed: $($_.Exception.Message)"
    }
  } else {
    $script:isRecording = $false
    $btnMic.IsEnabled = $false
    $btnMic.Content = "⚡ Processando..."
    $statusText.Text = "Transcrevendo..."
    try {
      if ($script:recProc -and -not $script:recProc.HasExited) {
        $script:recProc.StandardInput.WriteLine("q")
        if (-not $script:recProc.WaitForExit(1200)) {
          $script:recProc.Kill()
        }
      }
    } catch {}
    $script:recProc = $null

    if ($script:recAudioPath -and (Test-Path -LiteralPath $script:recAudioPath)) {
      $audioLen = (Get-Item -LiteralPath $script:recAudioPath).Length
      Write-DebugLog "Recorded MP3 bytes=$audioLen"
      $script:sttOutTxt = "$($script:recAudioPath).txt"
      $script:sttStartTime = [DateTime]::UtcNow

      $sttPsi = New-Object System.Diagnostics.ProcessStartInfo
      $sttPsi.FileName = $NodePath
      $sttPsi.Arguments = "`"$TranscribeScript`" `"$($script:recAudioPath)`" `"$($script:sttOutTxt)`""
      $sttPsi.UseShellExecute = $false
      $sttPsi.CreateNoWindow = $true
      $script:sttProc = [System.Diagnostics.Process]::Start($sttPsi)
    } else {
      Write-DebugLog "Recorded MP3 missing!"
      $statusText.Text = "Áudio vazio"
      $btnMic.Content = "🎤 Falar"
      $btnMic.Background = (New-Object System.Windows.Media.BrushConverter).ConvertFromString("#0A84FF")
      $outerBorder.BorderBrush = (New-Object System.Windows.Media.BrushConverter).ConvertFromString("#0A84FF")
      $avatarFrame.BorderBrush = (New-Object System.Windows.Media.BrushConverter).ConvertFromString("#0A84FF")
      $btnMic.IsEnabled = $true
    }
  }
})

# Timer: heartbeat + async STT completion + WAV queue playback + 16ms (60 FPS) F1/F2 Phonetic-Viseme lip-sync with smooth crossfade & strict silence gate
$timer = New-Object System.Windows.Threading.DispatcherTimer
$timer.Interval = [TimeSpan]::FromMilliseconds(16)
$timer.Add_Tick({
  $script:pulseTick++

  # Write heartbeat every ~1s so voice-narrator.mjs knows companion is alive
  if ($script:pulseTick % 60 -eq 0) {
    try { [DateTimeOffset]::UtcNow.ToUnixTimeSeconds().ToString() | Set-Content -LiteralPath $heartbeatPath -Encoding ASCII } catch {}
  }

  # Handle async STT process completion (zero UI freeze!)
  if ($script:sttProc) {
    if ($script:sttProc.HasExited) {
      $exitCode = $script:sttProc.ExitCode
      $elapsedMs = [int]([DateTime]::UtcNow - $script:sttStartTime).TotalMilliseconds
      $script:sttProc = $null

      $transcribed = ""
      if ($script:sttOutTxt -and (Test-Path -LiteralPath $script:sttOutTxt)) {
        $transcribed = (Get-Content -LiteralPath $script:sttOutTxt -Raw -Encoding UTF8).Trim()
        try { Remove-Item -LiteralPath $script:sttOutTxt -Force -ErrorAction SilentlyContinue } catch {}
      }
      if ($script:recAudioPath -and (Test-Path -LiteralPath $script:recAudioPath)) {
        try { Remove-Item -LiteralPath $script:recAudioPath -Force -ErrorAction SilentlyContinue } catch {}
      }

      Write-DebugLog "Async STT done in ${elapsedMs}ms exit=$exitCode text='$transcribed'"

      if ($exitCode -eq 0 -and $transcribed) {
        $preview = if ($transcribed.Length -gt 26) { $transcribed.Substring(0, 26) + "..." } else { $transcribed }
        $statusText.Text = $preview
        Send-TextToTerminal $transcribed
      } else {
        $statusText.Text = "Não ouvi fala clara"
      }

      $btnMic.Content = "🎤 Falar"
      $btnMic.Background = (New-Object System.Windows.Media.BrushConverter).ConvertFromString("#0A84FF")
      $outerBorder.BorderBrush = (New-Object System.Windows.Media.BrushConverter).ConvertFromString("#0A84FF")
      $avatarFrame.BorderBrush = (New-Object System.Windows.Media.BrushConverter).ConvertFromString("#0A84FF")
      $btnMic.IsEnabled = $true
    } else {
      $sec = [Math]::Round(([DateTime]::UtcNow - $script:sttStartTime).TotalSeconds, 1)
      $statusText.Text = "Transcrevendo (${sec}s)..."
    }
  }

  # Clean finished playback
  if ($script:playProc -and $script:playProc.HasExited) {
    Stop-Playback
    if (-not $script:isRecording) {
      $statusText.Text = "Pronta para ouvir"
    }
  }

  # Pick up next queued WAV if idle & voice enabled
  if (-not $script:isRecording -and -not $script:playProc) {
    $nextFile = Get-ChildItem -LiteralPath $queueDir -Filter "*.wav" -ErrorAction SilentlyContinue |
      Sort-Object CreationTime | Select-Object -First 1
    if ($nextFile) {
      if ($script:voiceEnabled -and (Test-Path -LiteralPath $FfplayPath)) {
        Start-WavPlayback $nextFile.FullName $true
      } else {
        try { Remove-Item -LiteralPath $nextFile.FullName -Force -ErrorAction SilentlyContinue } catch {}
      }
    }
  }

  # 60 FPS Phonetic-Viseme F1/F2 lip-sync + smooth sub-frame crossfade + strict silence gate + natural eye blink
  $nowUtc = [DateTime]::UtcNow
  $tSec = $script:pulseTick * 0.01667
  if ($nowUtc -ge $script:nextBlinkAt -and $script:blinkTick -lt 0) {
    $script:blinkTick = 0
    $script:nextBlinkAt = $nowUtc.AddMilliseconds($script:rng.Next(3000, 5800))
  }
  $blinkFrameIdx = -1
  if ($script:blinkTick -ge 0) {
    if ($script:blinkTick -lt $script:blinkSeq.Length) {
      $blinkFrameIdx = $script:blinkSeq[$script:blinkTick]
      $script:blinkTick++
    } else {
      $script:blinkTick = -1
    }
  }

  $bright = (($script:pulseTick / 12) % 2 -lt 1)
  if ($script:isRecording) {
    $statusDot.Fill = (New-Object System.Windows.Media.BrushConverter).ConvertFromString("#FF3B30")
    $statusDot.Opacity = if ($bright) { 1.0 } else { 0.3 }
    $script:smoothMouth = 0.0
    $recBrush = if ($blinkFrameIdx -ge 0 -and $script:blinkFlowBrushes.Count -gt $blinkFrameIdx) {
      $script:blinkFlowBrushes[$blinkFrameIdx]
    } elseif ($script:lipFlowBrushes.Count -gt 0) {
      $script:lipFlowBrushes[0]
    } else {
      $script:brushIdle
    }
    Set-AvatarPoseSmooth $recBrush 0.60
    $avatarRotate.Angle = 1.2 * [Math]::Sin($tSec * 1.4)
    $avatarScale.ScaleX = 1.012 + 0.006 * [Math]::Sin($tSec * 2.0)
    $avatarScale.ScaleY = $avatarScale.ScaleX
    $avatarTranslate.Y = -1.0 * [Math]::Sin($tSec * 1.4)
  } elseif ($script:playProc -and -not $script:playProc.HasExited) {
    $statusDot.Fill = (New-Object System.Windows.Media.BrushConverter).ConvertFromString("#0A84FF")
    $statusDot.Opacity = if ($bright) { 1.0 } else { 0.35 }
    $visemeLevel = 0
    $elapsedMs = $script:playWatch.ElapsedMilliseconds
    if ($script:lipSyncFrames -and $script:lipSyncFrames.Length -gt 0) {
      # Compensação calibrada de ~75ms de latência do buffer de saída ffplay/WASAPI a 60 FPS
      $idx = [int](([Math]::Max(0, $elapsedMs - 75) * $script:lipSyncFps) / 1000)
      if ($idx -lt $script:lipSyncFrames.Length) {
        $visemeLevel = [int]$script:lipSyncFrames[$idx]
      }
    }
    $amp = $visemeLevel / 15.0
    # Subtle natural head/neck sway & breathing synced with speech energy
    $avatarRotate.Angle = (1.4 * [Math]::Sin($tSec * 1.7)) + ($amp * 0.8 * [Math]::Cos($tSec * 2.9))
    $avatarScale.ScaleX = 1.0 + (0.009 * [Math]::Sin($tSec * 1.9)) + ($amp * 0.012)
    $avatarScale.ScaleY = $avatarScale.ScaleX
    $avatarTranslate.Y = -1.2 * [Math]::Sin($tSec * 2.1)

    $borderW = 2.2 + [Math]::Min(1.6, $amp * 2.6)
    $avatarFrame.BorderThickness = [System.Windows.Thickness]::new($borderW)

    # Gate de silêncio rígido + Crossfade suave a 60 FPS nos 16 frames lip_01..lip_16
    $maxLip = [Math]::Max(1, $script:lipFlowBrushes.Count - 1)
    $targetLip = [Math]::Max(0, [Math]::Min($maxLip, $visemeLevel))
    if ($targetLip -eq 0) {
      # Fechamento imediato entre palavras (sem arrasto de inércia!)
      $script:smoothMouth = 0.0
      $lipIdx = 0
      $fadeStep = 0.78
    } else {
      # Articulação fonética direta com leve suavização para 60 FPS
      $script:smoothMouth = ($script:smoothMouth * 0.25) + ($targetLip * 0.75)
      $lipIdx = [Math]::Max(1, [Math]::Min($maxLip, [int][Math]::Round($script:smoothMouth)))
      $fadeStep = 0.48
    }

    $nextBrush = if ($lipIdx -eq 0 -and $blinkFrameIdx -ge 0 -and $script:blinkFlowBrushes.Count -gt $blinkFrameIdx) {
      $script:blinkFlowBrushes[$blinkFrameIdx]
    } elseif ($script:lipFlowBrushes.Count -gt $lipIdx) {
      $script:lipFlowBrushes[$lipIdx]
    } else {
      $script:brushTalkMid
    }
    Set-AvatarPoseSmooth $nextBrush $fadeStep
  } else {
    $statusDot.Fill = (New-Object System.Windows.Media.BrushConverter).ConvertFromString("#30D158")
    $statusDot.Opacity = 1.0
    $avatarFrame.BorderThickness = [System.Windows.Thickness]::new(2.0)

    # Gate imediato ao encerrar fala + piscada natural suave
    $script:smoothMouth = 0.0
    $idleBrush = if ($blinkFrameIdx -ge 0 -and $script:blinkFlowBrushes.Count -gt $blinkFrameIdx) {
      $script:blinkFlowBrushes[$blinkFrameIdx]
    } elseif ($script:lipFlowBrushes.Count -gt 0) {
      $script:lipFlowBrushes[0]
    } else {
      $script:brushIdle
    }
    Set-AvatarPoseSmooth $idleBrush 0.55

    # Gentle alive idle breathing & micro head movement
    $avatarRotate.Angle = 1.0 * [Math]::Sin($tSec * 0.8)
    $avatarScale.ScaleX = 1.0 + 0.008 * [Math]::Sin($tSec * 1.2)
    $avatarScale.ScaleY = $avatarScale.ScaleX
    $avatarTranslate.Y = -0.9 * [Math]::Sin($tSec * 0.9)
  }
})
Update-VoiceConfig $true
$timer.Start()

$null = $window.ShowDialog()
$timer.Stop()
Stop-Playback
try { if (Test-Path -LiteralPath $heartbeatPath) { Remove-Item -LiteralPath $heartbeatPath -Force -ErrorAction SilentlyContinue } } catch {}
try { $mutex.ReleaseMutex(); $mutex.Dispose() } catch {}
