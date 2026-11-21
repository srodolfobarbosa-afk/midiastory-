$ErrorActionPreference = "Stop"
Add-Type -AssemblyName PresentationCore
Add-Type -AssemblyName PresentationFramework
Add-Type -AssemblyName WindowsBase

$outDir = "C:\Users\rodolfo\Downloads\google-ads-midiastore"
New-Item -ItemType Directory -Force -Path $outDir | Out-Null
$baseHomePath = "C:\Users\rodolfo\midiastory-\docs\campaigns\store-screenshots\_base\home.png"

function Convert-HexToColor([string]$hex) {
    return [System.Windows.Media.ColorConverter]::ConvertFromString($hex)
}

function New-SolidBrush([string]$hex) {
    $b = New-Object System.Windows.Media.SolidColorBrush (Convert-HexToColor $hex)
    $b.Freeze()
    return $b
}

function Load-BitmapFrozen([string]$path) {
    $bytes = [System.IO.File]::ReadAllBytes($path)
    $ms = New-Object System.IO.MemoryStream (,$bytes)
    $bmp = New-Object System.Windows.Media.Imaging.BitmapImage
    $bmp.BeginInit()
    $bmp.CacheOption = [System.Windows.Media.Imaging.BitmapCacheOption]::OnLoad
    $bmp.StreamSource = $ms
    $bmp.EndInit()
    $bmp.Freeze()
    $ms.Dispose()
    return $bmp
}

function Save-ElementAsPng([System.Windows.FrameworkElement]$el, [int]$w, [int]$h, [string]$filePath) {
    $el.Width = $w
    $el.Height = $h
    $el.Measure((New-Object System.Windows.Size($w, $h)))
    $el.Arrange((New-Object System.Windows.Rect(0, 0, $w, $h)))
    $el.UpdateLayout()

    $rtb = New-Object System.Windows.Media.Imaging.RenderTargetBitmap($w, $h, 96, 96, [System.Windows.Media.PixelFormats]::Pbgra32)
    $rtb.Render($el)
    $enc = New-Object System.Windows.Media.Imaging.PngBitmapEncoder
    $enc.Frames.Add([System.Windows.Media.Imaging.BitmapFrame]::Create($rtb))
    $fs = [System.IO.File]::Open($filePath, [System.IO.FileMode]::Create, [System.IO.FileAccess]::Write)
    try {
        $enc.Save($fs)
    } finally {
        $fs.Dispose()
    }
    Write-Host "SAVED: $filePath ($w x $h)"
}

function New-MidiastoreLogoCanvas([double]$size, [bool]$rounded = $true) {
    $grid = New-Object System.Windows.Controls.Grid
    $grid.Width = $size
    $grid.Height = $size

    $bgGrad = New-Object System.Windows.Media.LinearGradientBrush
    $bgGrad.StartPoint = New-Object System.Windows.Point(0, 0)
    $bgGrad.EndPoint = New-Object System.Windows.Point(1, 1)
    $bgGrad.GradientStops.Add((New-Object System.Windows.Media.GradientStop (Convert-HexToColor "#0B1324"), 0.0))
    $bgGrad.GradientStops.Add((New-Object System.Windows.Media.GradientStop (Convert-HexToColor "#000000"), 0.55))
    $bgGrad.GradientStops.Add((New-Object System.Windows.Media.GradientStop (Convert-HexToColor "#07182E"), 1.0))

    $cr = if ($rounded) { $size * 0.22 } else { 0 }
    $outerBorder = New-Object System.Windows.Controls.Border
    $outerBorder.CornerRadius = New-Object System.Windows.CornerRadius($cr)
    $outerBorder.Background = $bgGrad
    if ($rounded) {
        $outerBorder.BorderBrush = New-SolidBrush "#330A84FF"
        $outerBorder.BorderThickness = New-Object System.Windows.Thickness(($size * 0.015))
    }
    $grid.Children.Add($outerBorder) | Out-Null

    $glow = New-Object System.Windows.Shapes.Ellipse
    $glow.Width = $size * 0.82
    $glow.Height = $size * 0.82
    $glowBrush = New-Object System.Windows.Media.RadialGradientBrush (Convert-HexToColor "#440A84FF"), (Convert-HexToColor "#000A84FF")
    $glow.Fill = $glowBrush
    $grid.Children.Add($glow) | Out-Null

    $vb = New-Object System.Windows.Controls.Viewbox
    $vb.Width = $size * 0.78
    $vb.Height = $size * 0.78
    $cv = New-Object System.Windows.Controls.Canvas
    $cv.Width = 108
    $cv.Height = 108

    $ringGrad = New-Object System.Windows.Media.LinearGradientBrush
    $ringGrad.StartPoint = New-Object System.Windows.Point(0, 0)
    $ringGrad.EndPoint = New-Object System.Windows.Point(1, 1)
    $ringGrad.GradientStops.Add((New-Object System.Windows.Media.GradientStop (Convert-HexToColor "#38BDF8"), 0.0))
    $ringGrad.GradientStops.Add((New-Object System.Windows.Media.GradientStop (Convert-HexToColor "#0A84FF"), 0.6))
    $ringGrad.GradientStops.Add((New-Object System.Windows.Media.GradientStop (Convert-HexToColor "#30D158"), 1.0))

    $trackCircle = New-Object System.Windows.Shapes.Ellipse
    $trackCircle.Width = 76
    $trackCircle.Height = 76
    [System.Windows.Controls.Canvas]::SetLeft($trackCircle, 16)
    [System.Windows.Controls.Canvas]::SetTop($trackCircle, 16)
    $trackCircle.Stroke = New-SolidBrush "#1F2937"
    $trackCircle.StrokeThickness = 7.5
    $cv.Children.Add($trackCircle) | Out-Null

    $arcPath = New-Object System.Windows.Shapes.Path
    $arcPath.Data = [System.Windows.Media.Geometry]::Parse("M 27.1,27.1 A 38,38 0 1 1 22.5,75.2")
    $arcPath.Stroke = $ringGrad
    $arcPath.StrokeThickness = 8
    $arcPath.StrokeStartLineCap = [System.Windows.Media.PenLineCap]::Round
    $arcPath.StrokeEndLineCap = [System.Windows.Media.PenLineCap]::Round
    $cv.Children.Add($arcPath) | Out-Null

    $mPath = New-Object System.Windows.Shapes.Path
    $mPath.Data = [System.Windows.Media.Geometry]::Parse("M 35,66 L 35,42 L 47.5,56 L 54,48.5 L 60.5,56 L 73,42 L 73,66")
    $mPath.Stroke = New-SolidBrush "#FFFFFF"
    $mPath.StrokeThickness = 6.8
    $mPath.StrokeStartLineCap = [System.Windows.Media.PenLineCap]::Round
    $mPath.StrokeEndLineCap = [System.Windows.Media.PenLineCap]::Round
    $mPath.StrokeLineJoin = [System.Windows.Media.PenLineJoin]::Round
    $cv.Children.Add($mPath) | Out-Null

    $starPath = New-Object System.Windows.Shapes.Path
    $starPath.Data = [System.Windows.Media.Geometry]::Parse("M 80,16 L 82.8,23.2 L 90,26 L 82.8,28.8 L 80,36 L 77.2,28.8 L 70,26 L 77.2,23.2 Z")
    $starPath.Fill = New-SolidBrush "#38BDF8"
    $cv.Children.Add($starPath) | Out-Null

    $vb.Child = $cv
    $grid.Children.Add($vb) | Out-Null
    return $grid
}

function New-PhoneMockupWithBadge([System.Windows.Media.Imaging.BitmapImage]$bmp, [double]$phoneW, [double]$phoneH, [string]$badgeLabel) {
    $wrap = New-Object System.Windows.Controls.Grid
    $wrap.Width = $phoneW + 24
    $wrap.Height = $phoneH

    $phoneBorder = New-Object System.Windows.Controls.Border
    $phoneBorder.Width = $phoneW
    $phoneBorder.Height = $phoneH
    $phoneBorder.CornerRadius = New-Object System.Windows.CornerRadius(30)
    $phoneBorder.Background = New-SolidBrush "#000000"
    $phoneBorder.BorderBrush = New-SolidBrush "#3B4A63"
    $phoneBorder.BorderThickness = New-Object System.Windows.Thickness(3.5)
    $phoneBorder.Padding = New-Object System.Windows.Thickness(7)

    $innerClip = New-Object System.Windows.Controls.Border
    $innerClip.CornerRadius = New-Object System.Windows.CornerRadius(23)
    $innerClip.ClipToBounds = $true
    $innerClip.Background = New-SolidBrush "#000000"

    $img = New-Object System.Windows.Controls.Image
    $img.Source = $bmp
    $img.Stretch = [System.Windows.Media.Stretch]::UniformToFill
    $img.HorizontalAlignment = [System.Windows.HorizontalAlignment]::Center
    $img.VerticalAlignment = [System.Windows.VerticalAlignment]::Top
    $innerClip.Child = $img
    $phoneBorder.Child = $innerClip
    $wrap.Children.Add($phoneBorder) | Out-Null

    # Floating dopamine relief pill at bottom of phone
    $floatPill = New-Object System.Windows.Controls.Border
    $floatPill.Background = New-SolidBrush "#0D2618"
    $floatPill.BorderBrush = New-SolidBrush "#30D158"
    $floatPill.BorderThickness = New-Object System.Windows.Thickness(1.5)
    $floatPill.CornerRadius = New-Object System.Windows.CornerRadius(14)
    $floatPill.Padding = New-Object System.Windows.Thickness(14, 7, 14, 7)
    $floatPill.HorizontalAlignment = [System.Windows.HorizontalAlignment]::Center
    $floatPill.VerticalAlignment = [System.Windows.VerticalAlignment]::Bottom
    $floatPill.Margin = New-Object System.Windows.Thickness(0, 0, 0, 18)

    $pillTxt = New-Object System.Windows.Controls.TextBlock
    $pillTxt.Text = $badgeLabel
    $pillTxt.FontFamily = New-Object System.Windows.Media.FontFamily "Segoe UI"
    $pillTxt.FontSize = 15
    $pillTxt.FontWeight = [System.Windows.FontWeights]::ExtraBold
    $pillTxt.Foreground = New-SolidBrush "#30D158"
    $floatPill.Child = $pillTxt
    $wrap.Children.Add($floatPill) | Out-Null

    return $wrap
}

$homeBmp = Load-BitmapFrozen $baseHomePath
$font = New-Object System.Windows.Media.FontFamily "Segoe UI"

function Render-HorizontalBanner(
    [string]$outFile,
    [string]$eyebrowText,
    [string]$eyebrowColor,
    [string]$eyebrowBg,
    [string]$headlineText,
    [string]$subtitleText,
    [string]$ctaTextStr,
    [string]$phoneBadgeStr
) {
    $horizRoot = New-Object System.Windows.Controls.Grid
    $hBg = New-Object System.Windows.Media.LinearGradientBrush
    $hBg.StartPoint = New-Object System.Windows.Point(0, 0)
    $hBg.EndPoint = New-Object System.Windows.Point(1, 1)
    $hBg.GradientStops.Add((New-Object System.Windows.Media.GradientStop (Convert-HexToColor "#060B16"), 0.0))
    $hBg.GradientStops.Add((New-Object System.Windows.Media.GradientStop (Convert-HexToColor "#000000"), 0.5))
    $hBg.GradientStops.Add((New-Object System.Windows.Media.GradientStop (Convert-HexToColor "#08172E"), 1.0))
    $horizRoot.Background = $hBg

    $hGlow = New-Object System.Windows.Shapes.Ellipse
    $hGlow.Width = 700
    $hGlow.Height = 560
    $hGlow.HorizontalAlignment = [System.Windows.HorizontalAlignment]::Right
    $hGlow.VerticalAlignment = [System.Windows.VerticalAlignment]::Center
    $hGlow.Margin = New-Object System.Windows.Thickness(0, 0, -40, 0)
    $hGlow.Fill = New-Object System.Windows.Media.RadialGradientBrush (Convert-HexToColor "#3D0A84FF"), (Convert-HexToColor "#000A84FF")
    $horizRoot.Children.Add($hGlow) | Out-Null

    # Safe-zone margins (76px left, 56px right, 48px top/bottom so Google Ads never clips text or button)
    $hCols = New-Object System.Windows.Controls.Grid
    $hCols.Margin = New-Object System.Windows.Thickness(76, 48, 56, 48)
    $c0 = New-Object System.Windows.Controls.ColumnDefinition
    $c0.Width = New-Object System.Windows.GridLength(1.55, [System.Windows.GridUnitType]::Star)
    $c1 = New-Object System.Windows.Controls.ColumnDefinition
    $c1.Width = New-Object System.Windows.GridLength(0.88, [System.Windows.GridUnitType]::Star)
    $hCols.ColumnDefinitions.Add($c0)
    $hCols.ColumnDefinitions.Add($c1)

    $leftStack = New-Object System.Windows.Controls.StackPanel
    $leftStack.VerticalAlignment = [System.Windows.VerticalAlignment]::Center

    # Top row: Brand + Emotional Trigger Pill
    $brandRow = New-Object System.Windows.Controls.StackPanel
    $brandRow.Orientation = [System.Windows.Controls.Orientation]::Horizontal
    $brandRow.Margin = New-Object System.Windows.Thickness(0, 0, 0, 14)
    $miniLogo = New-MidiastoreLogoCanvas 44 $true
    $brandRow.Children.Add($miniLogo) | Out-Null

    $brandText = New-Object System.Windows.Controls.TextBlock
    $brandText.FontFamily = $font
    $brandText.FontSize = 28
    $brandText.FontWeight = [System.Windows.FontWeights]::ExtraBold
    $brandText.VerticalAlignment = [System.Windows.VerticalAlignment]::Center
    $brandText.Margin = New-Object System.Windows.Thickness(12, 0, 16, 0)
    $run1 = New-Object System.Windows.Documents.Run("Midia")
    $run1.Foreground = New-SolidBrush "#FFFFFF"
    $run2 = New-Object System.Windows.Documents.Run("store")
    $run2.Foreground = New-SolidBrush "#0A84FF"
    $brandText.Inlines.Add($run1)
    $brandText.Inlines.Add($run2)
    $brandRow.Children.Add($brandText) | Out-Null

    $tagPill = New-Object System.Windows.Controls.Border
    $tagPill.Background = New-SolidBrush $eyebrowBg
    $tagPill.BorderBrush = New-SolidBrush $eyebrowColor
    $tagPill.BorderThickness = New-Object System.Windows.Thickness(1.5)
    $tagPill.CornerRadius = New-Object System.Windows.CornerRadius(12)
    $tagPill.Padding = New-Object System.Windows.Thickness(12, 5, 12, 5)
    $tagPill.VerticalAlignment = [System.Windows.VerticalAlignment]::Center
    $tagTxt = New-Object System.Windows.Controls.TextBlock
    $tagTxt.FontFamily = $font
    $tagTxt.Text = $eyebrowText
    $tagTxt.FontSize = 13
    $tagTxt.FontWeight = [System.Windows.FontWeights]::ExtraBold
    $tagTxt.Foreground = New-SolidBrush $eyebrowColor
    $tagPill.Child = $tagTxt
    $brandRow.Children.Add($tagPill) | Out-Null

    $leftStack.Children.Add($brandRow) | Out-Null

    $hTitle = New-Object System.Windows.Controls.TextBlock
    $hTitle.FontFamily = $font
    $hTitle.Text = $headlineText
    $hTitle.FontSize = 42
    $hTitle.LineHeight = 48
    $hTitle.FontWeight = [System.Windows.FontWeights]::ExtraBold
    $hTitle.Foreground = New-SolidBrush "#FFFFFF"
    $leftStack.Children.Add($hTitle) | Out-Null

    $hSub = New-Object System.Windows.Controls.TextBlock
    $hSub.FontFamily = $font
    $hSub.Text = $subtitleText
    $hSub.FontSize = 20
    $hSub.LineHeight = 27
    $hSub.Foreground = New-SolidBrush "#B4C0D3"
    $hSub.Margin = New-Object System.Windows.Thickness(0, 12, 0, 20)
    $leftStack.Children.Add($hSub) | Out-Null

    # Bottom row: CTA + Trust Check
    $ctaRow = New-Object System.Windows.Controls.StackPanel
    $ctaRow.Orientation = [System.Windows.Controls.Orientation]::Horizontal

    $ctaBadge = New-Object System.Windows.Controls.Border
    $ctaBadge.Background = New-SolidBrush "#0A84FF"
    $ctaBadge.CornerRadius = New-Object System.Windows.CornerRadius(12)
    $ctaBadge.Padding = New-Object System.Windows.Thickness(22, 12, 22, 12)
    $ctaText = New-Object System.Windows.Controls.TextBlock
    $ctaText.FontFamily = $font
    $ctaText.Text = $ctaTextStr
    $ctaText.FontSize = 19
    $ctaText.FontWeight = [System.Windows.FontWeights]::ExtraBold
    $ctaText.Foreground = New-SolidBrush "#FFFFFF"
    $ctaBadge.Child = $ctaText
    $ctaRow.Children.Add($ctaBadge) | Out-Null

    $trustTxt = New-Object System.Windows.Controls.TextBlock
    $trustTxt.FontFamily = $font
    $trustTxt.Text = "✓ Não apaga nada sozinho`n✓ Super leve e seguro"
    $trustTxt.FontSize = 14
    $trustTxt.LineHeight = 19
    $trustTxt.FontWeight = [System.Windows.FontWeights]::SemiBold
    $trustTxt.Foreground = New-SolidBrush "#30D158"
    $trustTxt.VerticalAlignment = [System.Windows.VerticalAlignment]::Center
    $trustTxt.Margin = New-Object System.Windows.Thickness(18, 0, 0, 0)
    $ctaRow.Children.Add($trustTxt) | Out-Null

    $leftStack.Children.Add($ctaRow) | Out-Null

    [System.Windows.Controls.Grid]::SetColumn($leftStack, 0)
    $hCols.Children.Add($leftStack) | Out-Null

    $rightBox = New-Object System.Windows.Controls.Grid
    $rightBox.HorizontalAlignment = [System.Windows.HorizontalAlignment]::Center
    $rightBox.VerticalAlignment = [System.Windows.VerticalAlignment]::Center
    $phoneHoriz = New-PhoneMockupWithBadge $homeBmp 270 500 $phoneBadgeStr
    $rightBox.Children.Add($phoneHoriz) | Out-Null
    [System.Windows.Controls.Grid]::SetColumn($rightBox, 1)
    $hCols.Children.Add($rightBox) | Out-Null

    $horizRoot.Children.Add($hCols) | Out-Null
    Save-ElementAsPng $horizRoot 1200 628 $outFile
}

function Render-SquareBanner(
    [string]$outFile,
    [string]$eyebrowText,
    [string]$eyebrowColor,
    [string]$eyebrowBg,
    [string]$headlineText,
    [string]$subtitleText,
    [string]$ctaTextStr,
    [string]$phoneBadgeStr
) {
    $sqRoot = New-Object System.Windows.Controls.Grid
    $sqBg = New-Object System.Windows.Media.LinearGradientBrush
    $sqBg.StartPoint = New-Object System.Windows.Point(0.5, 0)
    $sqBg.EndPoint = New-Object System.Windows.Point(0.5, 1)
    $sqBg.GradientStops.Add((New-Object System.Windows.Media.GradientStop (Convert-HexToColor "#060B16"), 0.0))
    $sqBg.GradientStops.Add((New-Object System.Windows.Media.GradientStop (Convert-HexToColor "#000000"), 0.5))
    $sqBg.GradientStops.Add((New-Object System.Windows.Media.GradientStop (Convert-HexToColor "#08172E"), 1.0))
    $sqRoot.Background = $sqBg

    $sqGlow = New-Object System.Windows.Shapes.Ellipse
    $sqGlow.Width = 880
    $sqGlow.Height = 680
    $sqGlow.HorizontalAlignment = [System.Windows.HorizontalAlignment]::Center
    $sqGlow.VerticalAlignment = [System.Windows.VerticalAlignment]::Bottom
    $sqGlow.Margin = New-Object System.Windows.Thickness(0, 0, 0, 20)
    $sqGlow.Fill = New-Object System.Windows.Media.RadialGradientBrush (Convert-HexToColor "#3D0A84FF"), (Convert-HexToColor "#000A84FF")
    $sqRoot.Children.Add($sqGlow) | Out-Null

    $sqStack = New-Object System.Windows.Controls.StackPanel
    $sqStack.Margin = New-Object System.Windows.Thickness(76, 62, 76, 52)
    $sqStack.HorizontalAlignment = [System.Windows.HorizontalAlignment]::Center

    # Top row: Brand + Emotional Trigger Pill
    $sqBrandRow = New-Object System.Windows.Controls.StackPanel
    $sqBrandRow.Orientation = [System.Windows.Controls.Orientation]::Horizontal
    $sqBrandRow.HorizontalAlignment = [System.Windows.HorizontalAlignment]::Center
    $sqBrandRow.Margin = New-Object System.Windows.Thickness(0, 0, 0, 16)
    $sqMiniLogo = New-MidiastoreLogoCanvas 48 $true
    $sqBrandRow.Children.Add($sqMiniLogo) | Out-Null

    $sqBrandText = New-Object System.Windows.Controls.TextBlock
    $sqBrandText.FontFamily = $font
    $sqBrandText.FontSize = 32
    $sqBrandText.FontWeight = [System.Windows.FontWeights]::ExtraBold
    $sqBrandText.VerticalAlignment = [System.Windows.VerticalAlignment]::Center
    $sqBrandText.Margin = New-Object System.Windows.Thickness(12, 0, 16, 0)
    $sr1 = New-Object System.Windows.Documents.Run("Midia")
    $sr1.Foreground = New-SolidBrush "#FFFFFF"
    $sr2 = New-Object System.Windows.Documents.Run("store")
    $sr2.Foreground = New-SolidBrush "#0A84FF"
    $sqBrandText.Inlines.Add($sr1)
    $sqBrandText.Inlines.Add($sr2)
    $sqBrandRow.Children.Add($sqBrandText) | Out-Null

    $sqTagPill = New-Object System.Windows.Controls.Border
    $sqTagPill.Background = New-SolidBrush $eyebrowBg
    $sqTagPill.BorderBrush = New-SolidBrush $eyebrowColor
    $sqTagPill.BorderThickness = New-Object System.Windows.Thickness(1.5)
    $sqTagPill.CornerRadius = New-Object System.Windows.CornerRadius(12)
    $sqTagPill.Padding = New-Object System.Windows.Thickness(14, 5, 14, 5)
    $sqTagPill.VerticalAlignment = [System.Windows.VerticalAlignment]::Center
    $sqTagTxt = New-Object System.Windows.Controls.TextBlock
    $sqTagTxt.FontFamily = $font
    $sqTagTxt.Text = $eyebrowText
    $sqTagTxt.FontSize = 15
    $sqTagTxt.FontWeight = [System.Windows.FontWeights]::ExtraBold
    $sqTagTxt.Foreground = New-SolidBrush $eyebrowColor
    $sqTagPill.Child = $sqTagTxt
    $sqBrandRow.Children.Add($sqTagPill) | Out-Null

    $sqStack.Children.Add($sqBrandRow) | Out-Null

    $sqTitle = New-Object System.Windows.Controls.TextBlock
    $sqTitle.FontFamily = $font
    $sqTitle.Text = $headlineText
    $sqTitle.FontSize = 52
    $sqTitle.LineHeight = 58
    $sqTitle.TextAlignment = [System.Windows.TextAlignment]::Center
    $sqTitle.FontWeight = [System.Windows.FontWeights]::ExtraBold
    $sqTitle.Foreground = New-SolidBrush "#FFFFFF"
    $sqStack.Children.Add($sqTitle) | Out-Null

    $sqSub = New-Object System.Windows.Controls.TextBlock
    $sqSub.FontFamily = $font
    $sqSub.Text = $subtitleText
    $sqSub.FontSize = 22
    $sqSub.LineHeight = 30
    $sqSub.TextAlignment = [System.Windows.TextAlignment]::Center
    $sqSub.Foreground = New-SolidBrush "#B4C0D3"
    $sqSub.Margin = New-Object System.Windows.Thickness(0, 12, 0, 20)
    $sqStack.Children.Add($sqSub) | Out-Null

    # Trust Chips Row
    $chipsRow = New-Object System.Windows.Controls.StackPanel
    $chipsRow.Orientation = [System.Windows.Controls.Orientation]::Horizontal
    $chipsRow.HorizontalAlignment = [System.Windows.HorizontalAlignment]::Center
    $chipsRow.Margin = New-Object System.Windows.Thickness(0, 0, 0, 22)

    foreach ($chipLabel in @("✓ Não apaga nada sozinho", "✓ Limpa vídeos do WhatsApp", "✓ Grátis na Google Play")) {
        $chipBorder = New-Object System.Windows.Controls.Border
        $chipBorder.Background = New-SolidBrush "#121C2E"
        $chipBorder.BorderBrush = New-SolidBrush "#263A5E"
        $chipBorder.BorderThickness = New-Object System.Windows.Thickness(1)
        $chipBorder.CornerRadius = New-Object System.Windows.CornerRadius(12)
        $chipBorder.Padding = New-Object System.Windows.Thickness(14, 6, 14, 6)
        $chipBorder.Margin = New-Object System.Windows.Thickness(5, 0, 5, 0)
        $cTxt = New-Object System.Windows.Controls.TextBlock
        $cTxt.FontFamily = $font
        $cTxt.Text = $chipLabel
        $cTxt.FontSize = 15
        $cTxt.FontWeight = [System.Windows.FontWeights]::Bold
        $cTxt.Foreground = New-SolidBrush "#38BDF8"
        $chipBorder.Child = $cTxt
        $chipsRow.Children.Add($chipBorder) | Out-Null
    }
    $sqStack.Children.Add($chipsRow) | Out-Null

    $phoneSq = New-PhoneMockupWithBadge $homeBmp 400 680 $phoneBadgeStr
    $phoneSq.HorizontalAlignment = [System.Windows.HorizontalAlignment]::Center
    $sqStack.Children.Add($phoneSq) | Out-Null

    $sqRoot.Children.Add($sqStack) | Out-Null
    Save-ElementAsPng $sqRoot 1200 1200 $outFile
}

# 1. LOGO QUADRADO 512x512
$logo512 = New-MidiastoreLogoCanvas 512 $false
Save-ElementAsPng $logo512 512 512 (Join-Path $outDir "logo-quadrado-512x512.png")

# 2. KIT PRINCIPAL (Dor Real Popular: "CELULAR CHEIO?" + "Acabe com a memória lotada sem perder suas fotos")
Render-HorizontalBanner `
    -outFile (Join-Path $outDir "banner-horizontal-1200x628.png") `
    -eyebrowText "CELULAR CHEIO?" `
    -eyebrowColor "#30D158" `
    -eyebrowBg "#102819" `
    -headlineText "Acabe com a memória lotada`nsem perder suas fotos" `
    -subtitleText "Encontre fotos repetidas e vídeos pesados do WhatsApp`nem segundos. Você escolhe tudo antes de apagar." `
    -ctaTextStr "Baixar Grátis na Google Play  ›" `
    -phoneBadgeStr "✓ +14,8 GB prontos para liberar"

Render-SquareBanner `
    -outFile (Join-Path $outDir "banner-quadrado-1200x1200.png") `
    -eyebrowText "CELULAR CHEIO?" `
    -eyebrowColor "#30D158" `
    -eyebrowBg "#102819" `
    -headlineText "Acabe com a memória lotada`nsem perder suas fotos" `
    -subtitleText "Apague fotos repetidas e vídeos pesados do WhatsApp com segurança." `
    -ctaTextStr "Baixar Grátis na Google Play  ›" `
    -phoneBadgeStr "✓ +14,8 GB prontos para liberar"

# 3. KIT VARIAÇÃO 2 (Gatilho WhatsApp & Velocidade: "Seu celular leve de novo em segundos")
Render-HorizontalBanner `
    -outFile (Join-Path $outDir "banner-horizontal-2-whatsapp-1200x628.png") `
    -eyebrowText "WHATSAPP LOTADO?" `
    -eyebrowColor "#38BDF8" `
    -eyebrowBg "#0D2238" `
    -headlineText "Seu celular leve de novo`nem poucos segundos" `
    -subtitleText "Chega de aviso de armazenamento cheio. Limpe vídeos`ne fotos repetidas sem medo de apagar o que importa." `
    -ctaTextStr "Liberar Espaço Agora  ›" `
    -phoneBadgeStr "✓ 100% seguro no seu celular"

Render-SquareBanner `
    -outFile (Join-Path $outDir "banner-quadrado-2-whatsapp-1200x1200.png") `
    -eyebrowText "WHATSAPP LOTADO?" `
    -eyebrowColor "#38BDF8" `
    -eyebrowBg "#0D2238" `
    -headlineText "Seu celular leve de novo`nem poucos segundos" `
    -subtitleText "Chega de aviso de memória cheia. Limpe vídeos e fotos repetidas sem medo." `
    -ctaTextStr "Liberar Espaço Agora  ›" `
    -phoneBadgeStr "✓ 100% seguro no seu celular"
