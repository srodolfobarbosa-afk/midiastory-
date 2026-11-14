$ErrorActionPreference = "Stop"
Add-Type -AssemblyName PresentationCore, PresentationFramework, WindowsBase

$outPath = $env:MIDI_BOOK_COVER_OUT
if ([string]::IsNullOrWhiteSpace($outPath)) {
  throw "MIDI_BOOK_COVER_OUT not set"
}

function Get-EnvOrDefault([string]$name, [string]$fallback) {
  $val = [Environment]::GetEnvironmentVariable($name)
  if ([string]::IsNullOrWhiteSpace($val)) { return $fallback }
  return $val
}

$coverBadge = Get-EnvOrDefault "MIDI_COVER_BADGE" "EDIÇÃO OFICIAL • MIDIASTORE SERIES"
$coverT1    = Get-EnvOrDefault "MIDI_COVER_T1"    "CELULAR"
$coverT2    = Get-EnvOrDefault "MIDI_COVER_T2"    "LOTADO"
$coverT3    = Get-EnvOrDefault "MIDI_COVER_T3"    "NUNCA MAIS"
$coverSub   = Get-EnvOrDefault "MIDI_COVER_SUB"   "O Guia Definitivo da Memória Android: Como Recuperar Gigabytes Reais, Proteger Sua Privacidade EXIF/GPS e Evitar Apps Enganosos."
$coverHdr   = Get-EnvOrDefault "MIDI_COVER_HDR"   "DIAGNÓSTICO PRÁTICO EM 3 PILARES"
$coverFoot  = Get-EnvOrDefault "MIDI_COVER_FOOT"  "Inclui acesso às ferramentas web oficiais e guia de auditoria Android 10–16"
$coverEd    = Get-EnvOrDefault "MIDI_COVER_ED"    "EDIÇÃO COMPLETA • BR / GLOBAL"

$W = 1600
$H = 2560
$dpi = 96

function New-Color([string]$hex) {
  return [System.Windows.Media.ColorConverter]::ConvertFromString($hex)
}

function New-Brush([string]$hex) {
  $b = New-Object System.Windows.Media.SolidColorBrush (New-Color $hex)
  $b.Freeze()
  return $b
}

function New-Text([string]$txt, [double]$size, [System.Windows.FontWeight]$weight, [string]$colorHex, [double]$maxW, [string]$align = "Left") {
  $ff = New-Object System.Windows.Media.FontFamily "Segoe UI"
  $tf = New-Object System.Windows.Media.Typeface($ff, [System.Windows.FontStyles]::Normal, $weight, [System.Windows.FontStretches]::Normal)
  $ft = New-Object System.Windows.Media.FormattedText(
    $txt,
    [System.Globalization.CultureInfo]::GetCultureInfo("pt-BR"),
    [System.Windows.FlowDirection]::LeftToRight,
    $tf,
    $size,
    (New-Brush $colorHex),
    1.0
  )
  $ft.MaxTextWidth = $maxW
  $ft.LineHeight = $size * 1.18
  if ($align -eq "Center") {
    $ft.TextAlignment = [System.Windows.TextAlignment]::Center
  }
  return $ft
}

$dv = New-Object System.Windows.Media.DrawingVisual
[System.Windows.Media.TextOptions]::SetTextRenderingMode($dv, [System.Windows.Media.TextRenderingMode]::Grayscale)
[System.Windows.Media.TextOptions]::SetTextFormattingMode($dv, [System.Windows.Media.TextFormattingMode]::Ideal)
$dc = $dv.RenderOpen()

# Background Deep Navy-Emerald Editorial Gradient (clean 3-stop smooth gradient)
$bgGrad = New-Object System.Windows.Media.LinearGradientBrush
$bgGrad.StartPoint = New-Object System.Windows.Point(0, 0)
$bgGrad.EndPoint = New-Object System.Windows.Point(1, 1)
$bgGrad.GradientStops.Add((New-Object System.Windows.Media.GradientStop((New-Color "#FF040A14"), 0.0)))
$bgGrad.GradientStops.Add((New-Object System.Windows.Media.GradientStop((New-Color "#FF082235"), 0.55)))
$bgGrad.GradientStops.Add((New-Object System.Windows.Media.GradientStop((New-Color "#FF050D1A"), 1.0)))
$bgGrad.Freeze()
$dc.DrawRectangle($bgGrad, $null, (New-Object System.Windows.Rect(0, 0, $W, $H)))

# Top subtle mint radial glow
$glow = New-Object System.Windows.Media.RadialGradientBrush((New-Color "#325DE3BF"), (New-Color "#005DE3BF"))
$glow.Freeze()
$dc.DrawEllipse($glow, $null, (New-Object System.Windows.Point(1180, 420)), 720, 720)

$glow2 = New-Object System.Windows.Media.RadialGradientBrush((New-Color "#2419B8FF"), (New-Color "#0019B8FF"))
$glow2.Freeze()
$dc.DrawEllipse($glow2, $null, (New-Object System.Windows.Point(340, 2120)), 680, 680)

# Editorial gold/mint accent top border
$dc.DrawRectangle((New-Brush "#5DE3BF"), $null, (New-Object System.Windows.Rect(130, 120, 240, 10)))

# Eyebrow badge
$badgePen = New-Object System.Windows.Media.Pen((New-Brush "#5DE3BF"), 2.5)
$badgePen.Freeze()
$dc.DrawRoundedRectangle((New-Brush "#1A5DE3BF"), $badgePen, (New-Object System.Windows.Rect(130, 165, 620, 66)), 33, 33)
$ftBadge = New-Text $coverBadge 28 ([System.Windows.FontWeights]::Bold) "#5DE3BF" 590 "Center"
$dc.DrawText($ftBadge, (New-Object System.Windows.Point(145, 181)))

# Main Book Title
$ftTitle1 = New-Text $coverT1 144 ([System.Windows.FontWeights]::Black) "#FFFFFF" 1340 "Left"
$dc.DrawText($ftTitle1, (New-Object System.Windows.Point(130, 290)))

$ftTitle2 = New-Text $coverT2 144 ([System.Windows.FontWeights]::Black) "#5DE3BF" 1340 "Left"
$dc.DrawText($ftTitle2, (New-Object System.Windows.Point(130, 440)))

$ftTitle3 = New-Text $coverT3 138 ([System.Windows.FontWeights]::Black) "#FFFFFF" 1340 "Left"
$dc.DrawText($ftTitle3, (New-Object System.Windows.Point(130, 592)))

# Subtitle
$ftSub = New-Text $coverSub 44 ([System.Windows.FontWeights]::SemiBold) "#B7CAE4" 1320 "Left"
$dc.DrawText($ftSub, (New-Object System.Windows.Point(130, 790)))

# Center Editorial Feature Graphic (Minimalist Smartphone Card)
$cardPen = New-Object System.Windows.Media.Pen((New-Brush "#335DE3BF"), 3)
$cardPen.Freeze()
$dc.DrawRoundedRectangle((New-Brush "#D90E1D33"), $cardPen, (New-Object System.Windows.Rect(130, 1060, 1340, 920)), 44, 44)

# Inner Infographic Header
$ftCardHdr = New-Text $coverHdr 32 ([System.Windows.FontWeights]::Bold) "#5DE3BF" 1200 "Left"
$dc.DrawText($ftCardHdr, (New-Object System.Windows.Point(200, 1130)))

# Pillar bars
$pillars = @(
  @{ Y = 1230; Color = "#5DE3BF"; Label = (Get-EnvOrDefault "MIDI_COVER_P1_L" "01  DUPLICATAS EXATAS SHA-256 (BYTE A BYTE)"); Val = (Get-EnvOrDefault "MIDI_COVER_P1_V" "Zero risco ao original") },
  @{ Y = 1450; Color = "#38BDF8"; Label = (Get-EnvOrDefault "MIDI_COVER_P2_L" "02  PRIVACIDADE EXIF / GPS & SCOPED STORAGE"); Val = (Get-EnvOrDefault "MIDI_COVER_P2_V" "100% local no aparelho") },
  @{ Y = 1670; Color = "#FBBF24"; Label = (Get-EnvOrDefault "MIDI_COVER_P3_L" "03  WHATSAPP, VÍDEOS >10 MB & ESTÚDIO DE MÍDIA"); Val = (Get-EnvOrDefault "MIDI_COVER_P3_V" "M4A + PDF A4 + QR") }
)

foreach ($p in $pillars) {
  $py = [double]$p.Y
  $yLabel = $py + 34
  $yVal = $py + 96
  $rectCard = New-Object System.Windows.Rect(200, $py, 1200, 175)
  $rectBar = New-Object System.Windows.Rect(200, $py, 18, 175)
  $ptLabel = New-Object System.Windows.Point(255, $yLabel)
  $ptVal = New-Object System.Windows.Point(255, $yVal)
  $dc.DrawRoundedRectangle((New-Brush "#FF13263E"), $null, $rectCard, 24, 24)
  $dc.DrawRoundedRectangle((New-Brush $p.Color), $null, $rectBar, 6, 6)
  $ftPL = New-Text $p.Label 34 ([System.Windows.FontWeights]::Bold) "#FFFFFF" 1100 "Left"
  $dc.DrawText($ftPL, $ptLabel)
  $ftPV = New-Text $p.Val 32 ([System.Windows.FontWeights]::SemiBold) $p.Color 1100 "Left"
  $dc.DrawText($ftPV, $ptVal)
}

# Bottom Author & Brand Strip
$dc.DrawRectangle((New-Brush "#263B59"), $null, (New-Object System.Windows.Rect(130, 2140, 1340, 3)))

$ftBrand = New-Text "MIDIASTORE ENGINEERING" 48 ([System.Windows.FontWeights]::Black) "#FFFFFF" 1340 "Left"
$dc.DrawText($ftBrand, (New-Object System.Windows.Point(130, 2195)))

$ftFoot = New-Text $coverFoot 32 ([System.Windows.FontWeights]::Normal) "#9AB0CE" 1340 "Left"
$dc.DrawText($ftFoot, (New-Object System.Windows.Point(130, 2275)))

$dc.DrawRoundedRectangle((New-Brush "#5DE3BF"), $null, (New-Object System.Windows.Rect(130, 2365, 520, 74)), 18, 18)
$ftEdition = New-Text $coverEd 28 ([System.Windows.FontWeights]::Black) "#041019" 500 "Center"
$dc.DrawText($ftEdition, (New-Object System.Windows.Point(140, 2383)))

$dc.Close()

$rtb = New-Object System.Windows.Media.Imaging.RenderTargetBitmap($W, $H, $dpi, $dpi, [System.Windows.Media.PixelFormats]::Pbgra32)
$rtb.Render($dv)

$enc = New-Object System.Windows.Media.Imaging.PngBitmapEncoder
$enc.Frames.Add([System.Windows.Media.Imaging.BitmapFrame]::Create($rtb))

$dir = Split-Path -Parent $outPath
if (-not (Test-Path $dir)) { New-Item -ItemType Directory -Path $dir | Out-Null }

$fs = [System.IO.File]::Open($outPath, [System.IO.FileMode]::Create, [System.IO.FileAccess]::Write)
try {
  $enc.Save($fs)
} finally {
  $fs.Close()
}
Write-Output "COVER_OK: $outPath"
