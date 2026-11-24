$ErrorActionPreference = "Stop"
try { [Console]::OutputEncoding = [System.Text.Encoding]::UTF8 } catch {}

Add-Type -AssemblyName PresentationCore
Add-Type -AssemblyName PresentationFramework
Add-Type -AssemblyName WindowsBase

$Root = Resolve-Path (Join-Path $PSScriptRoot "..")
$QaDir = Join-Path $Root ".tmp\qa"
$BaseDir = Join-Path $Root "docs\campaigns\store-screenshots\_base"
$Dot = [string][char]0x2022

if (-not (Test-Path $BaseDir)) {
    New-Item -ItemType Directory -Path $BaseDir -Force | Out-Null
}

foreach ($old in @("home.png", "pro_privacy.png", "review_result.png", "trash_safety.png")) {
    $oldPath = Join-Path $BaseDir $old
    if (Test-Path $oldPath) { Remove-Item -Force $oldPath }
}

Copy-Item -Force (Join-Path $QaDir "portrait_tab1_limpar.png") (Join-Path $BaseDir "home_pt.png")
Copy-Item -Force (Join-Path $QaDir "en_tab1.png")              (Join-Path $BaseDir "home_en.png")
Copy-Item -Force (Join-Path $QaDir "portrait_tab3_pro.png")    (Join-Path $BaseDir "pro_pt.png")
Copy-Item -Force (Join-Path $QaDir "en_tab3_pro.png")          (Join-Path $BaseDir "pro_en.png")

function Convert-HexToColor([string]$hex) {
    return [System.Windows.Media.ColorConverter]::ConvertFromString($hex)
}

function New-SolidBrush([string]$hex) {
    $brush = New-Object System.Windows.Media.SolidColorBrush (Convert-HexToColor $hex)
    $brush.Freeze()
    return $brush
}

function Load-BitmapFrozen([string]$imagePath) {
    $bytes = [System.IO.File]::ReadAllBytes($imagePath)
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

function Save-ElementAsPng([System.Windows.FrameworkElement]$el, [int]$w, [int]$h, [string]$outPath) {
    [System.Windows.Media.TextOptions]::SetTextFormattingMode($el, [System.Windows.Media.TextFormattingMode]::Ideal)
    [System.Windows.Media.TextOptions]::SetTextRenderingMode($el, [System.Windows.Media.TextRenderingMode]::Grayscale)
    $el.SnapsToDevicePixels = $true

    $size = New-Object System.Windows.Size($w, $h)
    $el.Measure($size)
    $el.Arrange((New-Object System.Windows.Rect(0, 0, $w, $h)))
    $el.UpdateLayout()

    $rtb = New-Object System.Windows.Media.Imaging.RenderTargetBitmap($w, $h, 96, 96, [System.Windows.Media.PixelFormats]::Pbgra32)
    $rtb.Render($el)

    $encoder = New-Object System.Windows.Media.Imaging.PngBitmapEncoder
    $encoder.Frames.Add([System.Windows.Media.Imaging.BitmapFrame]::Create($rtb))
    $fs = [System.IO.File]::Open($outPath, [System.IO.FileMode]::Create)
    try {
        $encoder.Save($fs)
    } finally {
        $fs.Dispose()
    }
}

function New-VideoThumb([string]$c1, [string]$c2, [string]$tagText) {
    $thumb = New-Object System.Windows.Controls.Border
    $thumb.Width = 118
    $thumb.Height = 118
    $thumb.CornerRadius = New-Object System.Windows.CornerRadius(20)
    $grad = New-Object System.Windows.Media.LinearGradientBrush
    $grad.StartPoint = New-Object System.Windows.Point(0, 0)
    $grad.EndPoint = New-Object System.Windows.Point(1, 1)
    $grad.GradientStops.Add((New-Object System.Windows.Media.GradientStop (Convert-HexToColor $c1), 0.0))
    $grad.GradientStops.Add((New-Object System.Windows.Media.GradientStop (Convert-HexToColor $c2), 1.0))
    $thumb.Background = $grad
    $thumb.BorderBrush = New-SolidBrush "#33FFFFFF"
    $thumb.BorderThickness = New-Object System.Windows.Thickness(1)

    $inner = New-Object System.Windows.Controls.Grid
    $circle = New-Object System.Windows.Shapes.Ellipse
    $circle.Width = 50
    $circle.Height = 50
    $circle.Fill = New-SolidBrush "#48000000"
    $circle.Stroke = New-SolidBrush "#DDFFFFFF"
    $circle.StrokeThickness = 2
    $circle.HorizontalAlignment = [System.Windows.HorizontalAlignment]::Center
    $circle.VerticalAlignment = [System.Windows.VerticalAlignment]::Center
    $inner.Children.Add($circle) | Out-Null

    $playPoly = New-Object System.Windows.Shapes.Polygon
    $pts = New-Object System.Windows.Media.PointCollection
    $pts.Add((New-Object System.Windows.Point(53, 48)))
    $pts.Add((New-Object System.Windows.Point(53, 70)))
    $pts.Add((New-Object System.Windows.Point(71, 59)))
    $playPoly.Points = $pts
    $playPoly.Fill = New-SolidBrush "#FFFFFF"
    $inner.Children.Add($playPoly) | Out-Null

    $badge = New-Object System.Windows.Controls.Border
    $badge.Background = New-SolidBrush "#BB000000"
    $badge.CornerRadius = New-Object System.Windows.CornerRadius(7)
    $badge.Padding = New-Object System.Windows.Thickness(7, 2, 7, 2)
    $badge.HorizontalAlignment = [System.Windows.HorizontalAlignment]::Right
    $badge.VerticalAlignment = [System.Windows.VerticalAlignment]::Bottom
    $badge.Margin = New-Object System.Windows.Thickness(0, 0, 7, 7)
    $bt = New-Object System.Windows.Controls.TextBlock
    $bt.Text = $tagText
    $bt.FontFamily = New-Object System.Windows.Media.FontFamily "Segoe UI"
    $bt.FontSize = 14
    $bt.FontWeight = [System.Windows.FontWeights]::Bold
    $bt.Foreground = New-SolidBrush "#FFFFFF"
    $badge.Child = $bt
    $inner.Children.Add($badge) | Out-Null

    $thumb.Child = $inner
    return $thumb
}

function New-Checkbox([bool]$checked) {
    $box = New-Object System.Windows.Controls.Border
    $box.Width = 48
    $box.Height = 48
    $box.CornerRadius = New-Object System.Windows.CornerRadius(10)
    $box.VerticalAlignment = [System.Windows.VerticalAlignment]::Center
    if ($checked) {
        $box.Background = New-SolidBrush "#0A84FF"
        $box.BorderBrush = New-SolidBrush "#0A84FF"
        $box.BorderThickness = New-Object System.Windows.Thickness(2)
        $path = New-Object System.Windows.Shapes.Path
        $path.Data = [System.Windows.Media.Geometry]::Parse("M 12,24 L 20,32 L 36,15")
        $path.Stroke = New-SolidBrush "#FFFFFF"
        $path.StrokeThickness = 4.5
        $path.StrokeStartLineCap = [System.Windows.Media.PenLineCap]::Round
        $path.StrokeEndLineCap = [System.Windows.Media.PenLineCap]::Round
        $path.StrokeLineJoin = [System.Windows.Media.PenLineJoin]::Round
        $box.Child = $path
    } else {
        $box.Background = New-SolidBrush "#00000000"
        $box.BorderBrush = New-SolidBrush "#8E8E93"
        $box.BorderThickness = New-Object System.Windows.Thickness(3.5)
    }
    return $box
}

function New-FileRowCard([string]$fileName, [string]$metaPrimary, [string]$primaryHex, [string]$metaSecondary, [string]$c1, [string]$c2, [string]$tagText, [bool]$checked, [string]$pillRightText) {
    $card = New-Object System.Windows.Controls.Border
    $card.Width = 864
    $card.CornerRadius = New-Object System.Windows.CornerRadius(28)
    $card.Background = New-SolidBrush "#24262B"
    $card.BorderBrush = if ($checked) { New-SolidBrush "#660A84FF" } elseif ($pillRightText) { New-SolidBrush "#4530D158" } else { New-SolidBrush "#34363C" }
    $card.BorderThickness = New-Object System.Windows.Thickness(1.5)
    $card.Padding = New-Object System.Windows.Thickness(22, 18, 24, 18)

    $grid = New-Object System.Windows.Controls.Grid
    $col0 = New-Object System.Windows.Controls.ColumnDefinition; $col0.Width = New-Object System.Windows.GridLength(140)
    $col1 = New-Object System.Windows.Controls.ColumnDefinition; $col1.Width = New-Object System.Windows.GridLength(1, [System.Windows.GridUnitType]::Star)
    $col2 = New-Object System.Windows.Controls.ColumnDefinition; $col2.Width = [System.Windows.GridLength]::Auto
    $grid.ColumnDefinitions.Add($col0)
    $grid.ColumnDefinitions.Add($col1)
    $grid.ColumnDefinitions.Add($col2)

    $thumb = New-VideoThumb $c1 $c2 $tagText
    $thumb.HorizontalAlignment = [System.Windows.HorizontalAlignment]::Left
    [System.Windows.Controls.Grid]::SetColumn($thumb, 0)
    $grid.Children.Add($thumb) | Out-Null

    $stack = New-Object System.Windows.Controls.StackPanel
    $stack.VerticalAlignment = [System.Windows.VerticalAlignment]::Center
    $stack.Margin = New-Object System.Windows.Thickness(6, 0, 14, 0)

    $tName = New-Object System.Windows.Controls.TextBlock
    $tName.Text = $fileName
    $tName.FontFamily = New-Object System.Windows.Media.FontFamily "Segoe UI"
    $tName.FontSize = 27
    $tName.FontWeight = [System.Windows.FontWeights]::Bold
    $tName.Foreground = New-SolidBrush "#FFFFFF"
    $tName.TextTrimming = [System.Windows.TextTrimming]::CharacterEllipsis
    $stack.Children.Add($tName) | Out-Null

    $tMeta1 = New-Object System.Windows.Controls.TextBlock
    $tMeta1.Text = $metaPrimary
    $tMeta1.FontFamily = New-Object System.Windows.Media.FontFamily "Segoe UI"
    $tMeta1.FontSize = 23
    $tMeta1.FontWeight = [System.Windows.FontWeights]::Bold
    $tMeta1.Foreground = New-SolidBrush $primaryHex
    $tMeta1.Margin = New-Object System.Windows.Thickness(0, 4, 0, 0)
    $stack.Children.Add($tMeta1) | Out-Null

    $tMeta2 = New-Object System.Windows.Controls.TextBlock
    $tMeta2.Text = $metaSecondary
    $tMeta2.FontFamily = New-Object System.Windows.Media.FontFamily "Segoe UI"
    $tMeta2.FontSize = 21
    $tMeta2.Foreground = New-SolidBrush "#8E8E93"
    $tMeta2.Margin = New-Object System.Windows.Thickness(0, 3, 0, 0)
    $stack.Children.Add($tMeta2) | Out-Null

    [System.Windows.Controls.Grid]::SetColumn($stack, 1)
    $grid.Children.Add($stack) | Out-Null

    if ($pillRightText) {
        $pill = New-Object System.Windows.Controls.Border
        $pill.CornerRadius = New-Object System.Windows.CornerRadius(14)
        $pill.Background = New-SolidBrush "#2230D158"
        $pill.BorderBrush = New-SolidBrush "#7730D158"
        $pill.BorderThickness = New-Object System.Windows.Thickness(1.5)
        $pill.Padding = New-Object System.Windows.Thickness(14, 8, 14, 8)
        $pill.VerticalAlignment = [System.Windows.VerticalAlignment]::Center
        $pt = New-Object System.Windows.Controls.TextBlock
        $pt.Text = $pillRightText
        $pt.FontFamily = New-Object System.Windows.Media.FontFamily "Segoe UI"
        $pt.FontSize = 18
        $pt.FontWeight = [System.Windows.FontWeights]::Bold
        $pt.Foreground = New-SolidBrush "#30D158"
        $pill.Child = $pt
        [System.Windows.Controls.Grid]::SetColumn($pill, 2)
        $grid.Children.Add($pill) | Out-Null
    } else {
        $chk = New-Checkbox $checked
        [System.Windows.Controls.Grid]::SetColumn($chk, 2)
        $grid.Children.Add($chk) | Out-Null
    }

    $card.Child = $grid
    return $card
}

# =========================================================================
# 1. Build files_clean.png (1080x2400) sanitizing portrait_tab2_arquivos.png
# =========================================================================
$tab2Bmp = Load-BitmapFrozen (Join-Path $QaDir "portrait_tab2_arquivos.png")
$filesCanvas = New-Object System.Windows.Controls.Canvas
$filesCanvas.Width = 1080
$filesCanvas.Height = 2400
$filesCanvas.Background = New-SolidBrush "#000000"

# Keep authentic top section y=0..1748 (includes native "Selecionar página (100)" button!)
$topCrop = New-Object System.Windows.Media.Imaging.CroppedBitmap($tab2Bmp, (New-Object System.Windows.Int32Rect(0, 0, 1080, 1748)))
$topImg = New-Object System.Windows.Controls.Image
$topImg.Source = $topCrop
$topImg.Width = 1080
$topImg.Height = 1748
[System.Windows.Controls.Canvas]::SetLeft($topImg, 0)
[System.Windows.Controls.Canvas]::SetTop($topImg, 0)
$filesCanvas.Children.Add($topImg) | Out-Null

# Opaque sanitized block y=1748..2135 (100% covers any personal item below y=1748)
$cleanBlackMask = New-Object System.Windows.Shapes.Rectangle
$cleanBlackMask.Width = 1080
$cleanBlackMask.Height = 388
$cleanBlackMask.Fill = New-SolidBrush "#000000"
[System.Windows.Controls.Canvas]::SetLeft($cleanBlackMask, 0)
[System.Windows.Controls.Canvas]::SetTop($cleanBlackMask, 1748)
$filesCanvas.Children.Add($cleanBlackMask) | Out-Null

$outerCardContinue = New-Object System.Windows.Controls.Border
$outerCardContinue.Width = 968
$outerCardContinue.Height = 384
$outerCardContinue.ClipToBounds = $true
$outerCardContinue.Background = New-SolidBrush "#1C1C1E"
$outerCardContinue.CornerRadius = New-Object System.Windows.CornerRadius(0, 0, 44, 44)
[System.Windows.Controls.Canvas]::SetLeft($outerCardContinue, 56)
[System.Windows.Controls.Canvas]::SetTop($outerCardContinue, 1748)

$innerListStack = New-Object System.Windows.Controls.StackPanel
$innerListStack.Orientation = [System.Windows.Controls.Orientation]::Vertical
$innerListStack.HorizontalAlignment = [System.Windows.HorizontalAlignment]::Center
$innerListStack.Margin = New-Object System.Windows.Thickness(0, 14, 0, 0)

$vWord = "V$([char]0x00CD)DEO"
$row1 = New-FileRowCard "VID_Camera_4K_01.mp4" "142,4 MB  $Dot  $vWord" "#64D2FF" "DCIM/Camera  $Dot  3840x2160" "#0A84FF" "#4F46E5" "4K" $true ""
$row1.Margin = New-Object System.Windows.Thickness(0, 0, 0, 16)
$innerListStack.Children.Add($row1) | Out-Null

$row2 = New-FileRowCard "VID_WhatsApp_HD.mp4" "72,6 MB  $Dot  $vWord" "#BF5AF2" "Android/media  $Dot  +30d" "#7C3AED" "#0EA5E9" "HD" $true ""
$innerListStack.Children.Add($row2) | Out-Null

$outerCardContinue.Child = $innerListStack
$filesCanvas.Children.Add($outerCardContinue) | Out-Null

# Authentic bottom navigation bar y=2135..2400
$botCrop = New-Object System.Windows.Media.Imaging.CroppedBitmap($tab2Bmp, (New-Object System.Windows.Int32Rect(0, 2135, 1080, 265)))
$botImg = New-Object System.Windows.Controls.Image
$botImg.Source = $botCrop
$botImg.Width = 1080
$botImg.Height = 265
[System.Windows.Controls.Canvas]::SetLeft($botImg, 0)
[System.Windows.Controls.Canvas]::SetTop($botImg, 2135)
$filesCanvas.Children.Add($botImg) | Out-Null

Save-ElementAsPng $filesCanvas 1080 2400 (Join-Path $BaseDir "files_clean.png")
Write-Output "SAVED:files_clean.png"

# =========================================================================
# 2. Build review_clean.png (1080x2400) — Pure OLED SHA-256 Review & Trash
# =========================================================================
$enTab1Bmp = Load-BitmapFrozen (Join-Path $QaDir "en_tab1.png")
$revCanvas = New-Object System.Windows.Controls.Canvas
$revCanvas.Width = 1080
$revCanvas.Height = 2400
$revCanvas.Background = New-SolidBrush "#000000"

$statusCrop = New-Object System.Windows.Media.Imaging.CroppedBitmap($enTab1Bmp, (New-Object System.Windows.Int32Rect(0, 0, 1080, 115)))
$statusImg = New-Object System.Windows.Controls.Image
$statusImg.Source = $statusCrop
$statusImg.Width = 1080
$statusImg.Height = 115
[System.Windows.Controls.Canvas]::SetLeft($statusImg, 0)
[System.Windows.Controls.Canvas]::SetTop($statusImg, 0)
$revCanvas.Children.Add($statusImg) | Out-Null

$revStack = New-Object System.Windows.Controls.StackPanel
$revStack.Width = 968
$revStack.Orientation = [System.Windows.Controls.Orientation]::Vertical
[System.Windows.Controls.Canvas]::SetLeft($revStack, 56)
[System.Windows.Controls.Canvas]::SetTop($revStack, 136)

# Summary Card (#1C1C1E)
$sumCard = New-Object System.Windows.Controls.Border
$sumCard.CornerRadius = New-Object System.Windows.CornerRadius(44)
$sumCard.Background = New-SolidBrush "#1C1C1E"
$sumCard.BorderBrush = New-SolidBrush "#2C2C2E"
$sumCard.BorderThickness = New-Object System.Windows.Thickness(1.5)
$sumCard.Padding = New-Object System.Windows.Thickness(44, 34, 44, 34)
$sumCard.Margin = New-Object System.Windows.Thickness(0, 0, 0, 26)

$sumStack = New-Object System.Windows.Controls.StackPanel
$sumLabel = New-Object System.Windows.Controls.TextBlock
$sumLabel.Text = "EXACT SHA-256 DUPLICATES  $Dot  REVIEW & TRASH"
$sumLabel.FontFamily = New-Object System.Windows.Media.FontFamily "Segoe UI"
$sumLabel.FontSize = 22
$sumLabel.FontWeight = [System.Windows.FontWeights]::Bold
$sumLabel.Foreground = New-SolidBrush "#30D158"
$sumStack.Children.Add($sumLabel) | Out-Null

$sumVal = New-Object System.Windows.Controls.TextBlock
$sumVal.Text = "384.2 MB"
$sumVal.FontFamily = New-Object System.Windows.Media.FontFamily "Segoe UI"
$sumVal.FontSize = 68
$sumVal.FontWeight = [System.Windows.FontWeights]::Bold
$sumVal.Foreground = New-SolidBrush "#FFFFFF"
$sumVal.Margin = New-Object System.Windows.Thickness(0, 6, 0, 0)
$sumStack.Children.Add($sumVal) | Out-Null

$sumSub = New-Object System.Windows.Controls.TextBlock
$sumSub.Text = "2 byte-identical copies selected  $Dot  2 originals protected"
$sumSub.FontFamily = New-Object System.Windows.Media.FontFamily "Segoe UI"
$sumSub.FontSize = 24
$sumSub.FontWeight = [System.Windows.FontWeights]::SemiBold
$sumSub.Foreground = New-SolidBrush "#8E8E93"
$sumSub.Margin = New-Object System.Windows.Thickness(0, 4, 0, 24)
$sumStack.Children.Add($sumSub) | Out-Null

$ctaBtn = New-Object System.Windows.Controls.Border
$ctaBtn.Height = 112
$ctaBtn.CornerRadius = New-Object System.Windows.CornerRadius(34)
$ctaBtn.Background = New-SolidBrush "#0A84FF"
$ctaTxt = New-Object System.Windows.Controls.TextBlock
$ctaTxt.Text = "Move 2 copies to Trash  $Dot  384.2 MB"
$ctaTxt.FontFamily = New-Object System.Windows.Media.FontFamily "Segoe UI"
$ctaTxt.FontSize = 31
$ctaTxt.FontWeight = [System.Windows.FontWeights]::Bold
$ctaTxt.Foreground = New-SolidBrush "#FFFFFF"
$ctaTxt.HorizontalAlignment = [System.Windows.HorizontalAlignment]::Center
$ctaTxt.VerticalAlignment = [System.Windows.VerticalAlignment]::Center
$ctaBtn.Child = $ctaTxt
$sumStack.Children.Add($ctaBtn) | Out-Null

$sumCard.Child = $sumStack
$revStack.Children.Add($sumCard) | Out-Null

function New-DupGroupCard([string]$groupTitle, [string]$hashLabel, [string]$origName, [string]$copyName, [string]$sizeText, [string]$c1, [string]$c2, [string]$tag) {
    $gCard = New-Object System.Windows.Controls.Border
    $gCard.CornerRadius = New-Object System.Windows.CornerRadius(40)
    $gCard.Background = New-SolidBrush "#1C1C1E"
    $gCard.BorderBrush = New-SolidBrush "#2C2C2E"
    $gCard.BorderThickness = New-Object System.Windows.Thickness(1.5)
    $gCard.Padding = New-Object System.Windows.Thickness(44, 26, 44, 28)
    $gCard.Margin = New-Object System.Windows.Thickness(0, 0, 0, 24)

    $gs = New-Object System.Windows.Controls.StackPanel
    $hdr = New-Object System.Windows.Controls.DockPanel
    $hdr.Margin = New-Object System.Windows.Thickness(4, 0, 4, 16)

    $hLeft = New-Object System.Windows.Controls.TextBlock
    $hLeft.Text = $groupTitle
    $hLeft.FontFamily = New-Object System.Windows.Media.FontFamily "Segoe UI"
    $hLeft.FontSize = 26
    $hLeft.FontWeight = [System.Windows.FontWeights]::Bold
    $hLeft.Foreground = New-SolidBrush "#FFFFFF"
    [System.Windows.Controls.DockPanel]::SetDock($hLeft, [System.Windows.Controls.Dock]::Left)
    $hdr.Children.Add($hLeft) | Out-Null

    $hRight = New-Object System.Windows.Controls.TextBlock
    $hRight.Text = $hashLabel
    $hRight.FontFamily = New-Object System.Windows.Media.FontFamily "Consolas"
    $hRight.FontSize = 21
    $hRight.FontWeight = [System.Windows.FontWeights]::Bold
    $hRight.Foreground = New-SolidBrush "#FF9F0A"
    $hRight.HorizontalAlignment = [System.Windows.HorizontalAlignment]::Right
    $hRight.VerticalAlignment = [System.Windows.VerticalAlignment]::Center
    $hdr.Children.Add($hRight) | Out-Null

    $gs.Children.Add($hdr) | Out-Null

    $origRow = New-FileRowCard $origName "$sizeText  $Dot  ORIGINAL KEPT" "#30D158" "DCIM/Camera  $Dot  Protected" $c1 $c2 $tag $false "KEEP"
    $origRow.Margin = New-Object System.Windows.Thickness(0, 0, 0, 14)
    $gs.Children.Add($origRow) | Out-Null

    $copyRow = New-FileRowCard $copyName "$sizeText  $Dot  EXACT COPY" "#64D2FF" "Downloads  $Dot  Android Trash (30d)" $c1 $c2 $tag $true ""
    $gs.Children.Add($copyRow) | Out-Null

    $gCard.Child = $gs
    return $gCard
}

$g1 = New-DupGroupCard "SHA-256 Group #1" "sha256: 9f86d081...884c" "VID_Camera_4K_01.mp4" "VID_Camera_4K_01 (1).mp4" "248.0 MB" "#0A84FF" "#1D4ED8" "4K"
$revStack.Children.Add($g1) | Out-Null

$g2 = New-DupGroupCard "SHA-256 Group #2" "sha256: 4b227777...d419" "VID_Clips_1080p.mp4" "VID_Clips_1080p_copy.mp4" "136.2 MB" "#7C3AED" "#0EA5E9" "HD"
$revStack.Children.Add($g2) | Out-Null

# Android Trash Safety Banner at bottom of screen
$safeCard = New-Object System.Windows.Controls.Border
$safeCard.CornerRadius = New-Object System.Windows.CornerRadius(32)
$safeCard.Background = New-SolidBrush "#1430D158"
$safeCard.BorderBrush = New-SolidBrush "#4530D158"
$safeCard.BorderThickness = New-Object System.Windows.Thickness(1.5)
$safeCard.Padding = New-Object System.Windows.Thickness(36, 26, 36, 26)

$safeStack = New-Object System.Windows.Controls.StackPanel
$safeT1 = New-Object System.Windows.Controls.TextBlock
$safeT1.Text = "ANDROID 11+ SYSTEM TRASH PROTECTION"
$safeT1.FontFamily = New-Object System.Windows.Media.FontFamily "Segoe UI"
$safeT1.FontSize = 21
$safeT1.FontWeight = [System.Windows.FontWeights]::Bold
$safeT1.Foreground = New-SolidBrush "#30D158"
$safeStack.Children.Add($safeT1) | Out-Null

$safeT2 = New-Object System.Windows.Controls.TextBlock
$safeT2.Text = "Official Android confirmation dialog. Restore from Trash within 30 days."
$safeT2.FontFamily = New-Object System.Windows.Media.FontFamily "Segoe UI"
$safeT2.FontSize = 23
$safeT2.FontWeight = [System.Windows.FontWeights]::Medium
$safeT2.Foreground = New-SolidBrush "#D1D5DB"
$safeT2.TextWrapping = [System.Windows.TextWrapping]::Wrap
$safeT2.Margin = New-Object System.Windows.Thickness(0, 6, 0, 0)
$safeStack.Children.Add($safeT2) | Out-Null

$safeCard.Child = $safeStack
$revStack.Children.Add($safeCard) | Out-Null

$revCanvas.Children.Add($revStack) | Out-Null

$revBotCrop = New-Object System.Windows.Media.Imaging.CroppedBitmap($enTab1Bmp, (New-Object System.Windows.Int32Rect(0, 2135, 1080, 265)))
$revBotImg = New-Object System.Windows.Controls.Image
$revBotImg.Source = $revBotCrop
$revBotImg.Width = 1080
$revBotImg.Height = 265
[System.Windows.Controls.Canvas]::SetLeft($revBotImg, 0)
[System.Windows.Controls.Canvas]::SetTop($revBotImg, 2135)
$revCanvas.Children.Add($revBotImg) | Out-Null

Save-ElementAsPng $revCanvas 1080 2400 (Join-Path $BaseDir "review_clean.png")
Write-Output "SAVED:review_clean.png"
