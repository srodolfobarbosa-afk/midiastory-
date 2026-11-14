try {
$ManifestPath = $env:MIDI_SHOTS_MANIFEST
if ([string]::IsNullOrWhiteSpace($ManifestPath)) {
    throw "Missing MIDI_SHOTS_MANIFEST environment variable."
}

$ErrorActionPreference = "Stop"
try { [Console]::OutputEncoding = [System.Text.Encoding]::UTF8 } catch {}

Add-Type -AssemblyName PresentationCore
Add-Type -AssemblyName PresentationFramework
Add-Type -AssemblyName WindowsBase

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

$manifestRaw = [System.IO.File]::ReadAllText($ManifestPath, [System.Text.Encoding]::UTF8)
$parsed = $manifestRaw | ConvertFrom-Json
$items = @($parsed)

        $bitmapCache = @{}
        foreach ($item in $items) {
            if (-not $bitmapCache.ContainsKey($item.baseImagePath)) {
                $bitmapCache[$item.baseImagePath] = Load-BitmapFrozen $item.baseImagePath
            }
            $baseBitmap = $bitmapCache[$item.baseImagePath]

            $accentColor = Convert-HexToColor $item.accent
            $accentGlow38 = [System.Windows.Media.Color]::FromArgb(58, $accentColor.R, $accentColor.G, $accentColor.B)
            $accentGlow22 = [System.Windows.Media.Color]::FromArgb(34, $accentColor.R, $accentColor.G, $accentColor.B)
            $accentPillBg = [System.Windows.Media.Color]::FromArgb(36, $accentColor.R, $accentColor.G, $accentColor.B)
            $accentPillBorder = [System.Windows.Media.Color]::FromArgb(130, $accentColor.R, $accentColor.G, $accentColor.B)
            $transparentColor = [System.Windows.Media.Color]::FromArgb(0, $accentColor.R, $accentColor.G, $accentColor.B)

            $root = New-Object System.Windows.Controls.Grid
            $root.Width = 1080
            $root.Height = 1920
            $root.SnapsToDevicePixels = $true
            [System.Windows.Media.TextOptions]::SetTextFormattingMode($root, [System.Windows.Media.TextFormattingMode]::Ideal)
            [System.Windows.Media.TextOptions]::SetTextRenderingMode($root, [System.Windows.Media.TextRenderingMode]::ClearType)

            # Base vertical background gradient
            $bgBrush = New-Object System.Windows.Media.LinearGradientBrush
            $bgBrush.StartPoint = New-Object System.Windows.Point(0.5, 0)
            $bgBrush.EndPoint = New-Object System.Windows.Point(0.5, 1)
            $bgBrush.GradientStops.Add((New-Object System.Windows.Media.GradientStop (Convert-HexToColor "#07121A"), 0.0))
            $bgBrush.GradientStops.Add((New-Object System.Windows.Media.GradientStop (Convert-HexToColor "#050B11"), 0.52))
            $bgBrush.GradientStops.Add((New-Object System.Windows.Media.GradientStop (Convert-HexToColor "#081822"), 1.0))
            $root.Background = $bgBrush

            # Top accent radial glow
            $topGlow = New-Object System.Windows.Shapes.Ellipse
            $topGlow.Width = 1100
            $topGlow.Height = 680
            $topGlow.HorizontalAlignment = [System.Windows.HorizontalAlignment]::Center
            $topGlow.VerticalAlignment = [System.Windows.VerticalAlignment]::Top
            $topGlow.Margin = New-Object System.Windows.Thickness(0, -180, 0, 0)
            $topGlowBrush = New-Object System.Windows.Media.RadialGradientBrush $accentGlow38, $transparentColor
            $topGlow.Fill = $topGlowBrush
            $root.Children.Add($topGlow) | Out-Null

            # Phone halo glow
            $bottomGlow = New-Object System.Windows.Shapes.Ellipse
            $bottomGlow.Width = 900
            $bottomGlow.Height = 960
            $bottomGlow.HorizontalAlignment = [System.Windows.HorizontalAlignment]::Center
            $bottomGlow.VerticalAlignment = [System.Windows.VerticalAlignment]::Bottom
            $bottomGlow.Margin = New-Object System.Windows.Thickness(0, 0, 0, 120)
            $bottomGlowBrush = New-Object System.Windows.Media.RadialGradientBrush $accentGlow22, $transparentColor
            $bottomGlow.Fill = $bottomGlowBrush
            $root.Children.Add($bottomGlow) | Out-Null

            # Subtle geometric grid canvas
            $gridCanvas = New-Object System.Windows.Controls.Canvas
            $gridCanvas.Width = 1080
            $gridCanvas.Height = 1920
            $gridLineBrush = New-SolidBrush "#09FFFFFF"
            for ($x = 64; $x -lt 1080; $x += 64) {
                $vline = New-Object System.Windows.Shapes.Line
                $vline.X1 = $x; $vline.Y1 = 0; $vline.X2 = $x; $vline.Y2 = 960
                $vline.Stroke = $gridLineBrush
                $vline.StrokeThickness = 1
                $gridCanvas.Children.Add($vline) | Out-Null
            }
            for ($y = 64; $y -le 896; $y += 64) {
                $hline = New-Object System.Windows.Shapes.Line
                $hline.X1 = 0; $hline.Y1 = $y; $hline.X2 = 1080; $hline.Y2 = $y
                $hline.Stroke = $gridLineBrush
                $hline.StrokeThickness = 1
                $gridCanvas.Children.Add($hline) | Out-Null
            }
            $root.Children.Add($gridCanvas) | Out-Null

            $fontFamily = New-Object System.Windows.Media.FontFamily $item.wpfFont

            # Main vertical layout container
            $mainStack = New-Object System.Windows.Controls.StackPanel
            $mainStack.Orientation = [System.Windows.Controls.Orientation]::Vertical
            $mainStack.HorizontalAlignment = [System.Windows.HorizontalAlignment]::Stretch
            $mainStack.VerticalAlignment = [System.Windows.VerticalAlignment]::Top
            $mainStack.Margin = New-Object System.Windows.Thickness(56, 48, 56, 0)

            # Pill Badge
            $badgeBorder = New-Object System.Windows.Controls.Border
            $badgeBorder.HorizontalAlignment = [System.Windows.HorizontalAlignment]::Center
            $badgeBorder.CornerRadius = New-Object System.Windows.CornerRadius(20)
            $badgeBorder.Padding = New-Object System.Windows.Thickness(20, 9, 20, 9)
            $badgeBorder.Background = New-Object System.Windows.Media.SolidColorBrush $accentPillBg
            $badgeBorder.BorderBrush = New-Object System.Windows.Media.SolidColorBrush $accentPillBorder
            $badgeBorder.BorderThickness = New-Object System.Windows.Thickness(1.5)

            $badgeInner = New-Object System.Windows.Controls.StackPanel
            $badgeInner.Orientation = [System.Windows.Controls.Orientation]::Horizontal
            $badgeInner.VerticalAlignment = [System.Windows.VerticalAlignment]::Center

            $badgeDot = New-Object System.Windows.Shapes.Ellipse
            $badgeDot.Width = 10
            $badgeDot.Height = 10
            $badgeDot.Fill = New-Object System.Windows.Media.SolidColorBrush $accentColor
            $badgeDot.Margin = New-Object System.Windows.Thickness(0, 0, 10, 0)
            $badgeDot.VerticalAlignment = [System.Windows.VerticalAlignment]::Center
            $badgeInner.Children.Add($badgeDot) | Out-Null

            $badgeText = New-Object System.Windows.Controls.TextBlock
            $badgeText.Text = $item.badge
            $badgeText.FontFamily = $fontFamily
            $badgeText.FontSize = 19
            $badgeText.FontWeight = [System.Windows.FontWeights]::Bold
            $badgeText.Foreground = New-Object System.Windows.Media.SolidColorBrush $accentColor
            $badgeText.VerticalAlignment = [System.Windows.VerticalAlignment]::Center
            $badgeInner.Children.Add($badgeText) | Out-Null

            $badgeBorder.Child = $badgeInner
            $mainStack.Children.Add($badgeBorder) | Out-Null

            # Hero Headline
            $titleBlock = New-Object System.Windows.Controls.TextBlock
            $titleBlock.Text = $item.title
            $titleBlock.FontFamily = $fontFamily
            $titleBlock.FontSize = [double]$item.titleFontSize
            $titleBlock.FontWeight = [System.Windows.FontWeights]::Bold
            $titleBlock.Foreground = New-SolidBrush "#FFFFFF"
            $titleBlock.TextAlignment = [System.Windows.TextAlignment]::Center
            $titleBlock.TextWrapping = [System.Windows.TextWrapping]::Wrap
            $titleBlock.LineHeight = [double]$item.titleLineHeight
            $titleBlock.LineStackingStrategy = [System.Windows.LineStackingStrategy]::BlockLineHeight
            $titleBlock.MaxWidth = 948
            $titleBlock.HorizontalAlignment = [System.Windows.HorizontalAlignment]::Center
            $titleBlock.Margin = New-Object System.Windows.Thickness(0, 16, 0, 0)
            $mainStack.Children.Add($titleBlock) | Out-Null

            # Subtitle
            $subBlock = New-Object System.Windows.Controls.TextBlock
            $subBlock.Text = $item.subtitle
            $subBlock.FontFamily = $fontFamily
            $subBlock.FontSize = 24
            $subBlock.FontWeight = [System.Windows.FontWeights]::Medium
            $subBlock.Foreground = New-SolidBrush "#CBD5E1"
            $subBlock.TextAlignment = [System.Windows.TextAlignment]::Center
            $subBlock.TextWrapping = [System.Windows.TextWrapping]::Wrap
            $subBlock.LineHeight = 33
            $subBlock.LineStackingStrategy = [System.Windows.LineStackingStrategy]::BlockLineHeight
            $subBlock.MaxWidth = 880
            $subBlock.HorizontalAlignment = [System.Windows.HorizontalAlignment]::Center
            $subBlock.Margin = New-Object System.Windows.Thickness(0, 12, 0, 0)
            $mainStack.Children.Add($subBlock) | Out-Null

            # Feature Chips Row
            $chipsPanel = New-Object System.Windows.Controls.StackPanel
            $chipsPanel.Orientation = [System.Windows.Controls.Orientation]::Horizontal
            $chipsPanel.HorizontalAlignment = [System.Windows.HorizontalAlignment]::Center
            $chipsPanel.Margin = New-Object System.Windows.Thickness(0, 16, 0, 0)
            foreach ($chip in $item.chips) {
                $chipBorder = New-Object System.Windows.Controls.Border
                $chipBorder.CornerRadius = New-Object System.Windows.CornerRadius(17)
                $chipBorder.Padding = New-Object System.Windows.Thickness(16, 7, 16, 7)
                $chipBorder.Margin = New-Object System.Windows.Thickness(6, 0, 6, 0)
                $chipBorder.Background = New-SolidBrush "#DF0F1D27"
                $chipBorder.BorderBrush = New-SolidBrush "#4594A3B8"
                $chipBorder.BorderThickness = New-Object System.Windows.Thickness(1)

                $chipLabel = New-Object System.Windows.Controls.TextBlock
                $chipLabel.Text = $chip
                $chipLabel.FontFamily = $fontFamily
                $chipLabel.FontSize = 18
                $chipLabel.FontWeight = [System.Windows.FontWeights]::SemiBold
                $chipLabel.Foreground = New-SolidBrush "#E2E8F0"
                $chipBorder.Child = $chipLabel
                $chipsPanel.Children.Add($chipBorder) | Out-Null
            }
            $mainStack.Children.Add($chipsPanel) | Out-Null
            $root.Children.Add($mainStack) | Out-Null

            # Phone Frame anchored near bottom (clean 20:9 screen)
            $screenW = 621.0
            $screenH = 1380.0
            $bezelBorder = New-Object System.Windows.Controls.Border
            $bezelBorder.HorizontalAlignment = [System.Windows.HorizontalAlignment]::Center
            $bezelBorder.VerticalAlignment = [System.Windows.VerticalAlignment]::Bottom
            $bezelBorder.Margin = New-Object System.Windows.Thickness(0, 0, 0, 42)
            $bezelBorder.Padding = New-Object System.Windows.Thickness(13)
            $bezelBorder.CornerRadius = New-Object System.Windows.CornerRadius(52)
            $bezelBorder.BorderThickness = New-Object System.Windows.Thickness(1.5)
            $bezelBorder.BorderBrush = New-SolidBrush "#2AFFFFFF"

            $bezelBg = New-Object System.Windows.Media.LinearGradientBrush
            $bezelBg.StartPoint = New-Object System.Windows.Point(0, 0)
            $bezelBg.EndPoint = New-Object System.Windows.Point(1, 1)
            $bezelBg.GradientStops.Add((New-Object System.Windows.Media.GradientStop (Convert-HexToColor "#2D3E4E"), 0.0))
            $bezelBg.GradientStops.Add((New-Object System.Windows.Media.GradientStop (Convert-HexToColor "#101B24"), 0.5))
            $bezelBg.GradientStops.Add((New-Object System.Windows.Media.GradientStop (Convert-HexToColor "#1E303D"), 1.0))
            $bezelBorder.Background = $bezelBg

            $shadow = New-Object System.Windows.Media.Effects.DropShadowEffect
            $shadow.Color = Convert-HexToColor "#000000"
            $shadow.BlurRadius = 48
            $shadow.ShadowDepth = 18
            $shadow.Direction = 270
            $shadow.Opacity = 0.72
            $bezelBorder.Effect = $shadow

            $screenContainer = New-Object System.Windows.Controls.Grid
            $screenContainer.Width = $screenW
            $screenContainer.Height = $screenH
            $screenContainer.Background = New-SolidBrush "#071118"
            $clipRect = New-Object System.Windows.Media.RectangleGeometry (New-Object System.Windows.Rect(0, 0, $screenW, $screenH)), 40, 40
            $clipRect.Freeze()
            $screenContainer.Clip = $clipRect

            $img = New-Object System.Windows.Controls.Image
            $img.Source = $baseBitmap
            $img.Width = $screenW
            $img.Height = $screenH
            $img.Stretch = [System.Windows.Media.Stretch]::UniformToFill
            [System.Windows.Media.RenderOptions]::SetBitmapScalingMode($img, [System.Windows.Media.BitmapScalingMode]::HighQuality)
            $screenContainer.Children.Add($img) | Out-Null

            $bezelBorder.Child = $screenContainer
            $root.Children.Add($bezelBorder) | Out-Null

            # Footer Brand Watermark
            $footer = New-Object System.Windows.Controls.TextBlock
            $footer.Text = "Midiastore  $([char]0x2022)  com.rdf112.midiastore"
            $footer.FontFamily = New-Object System.Windows.Media.FontFamily "Segoe UI"
            $footer.FontSize = 15
            $footer.FontWeight = [System.Windows.FontWeights]::SemiBold
            $footer.Foreground = New-SolidBrush "#7594A3B8"
            $footer.HorizontalAlignment = [System.Windows.HorizontalAlignment]::Right
            $footer.VerticalAlignment = [System.Windows.VerticalAlignment]::Bottom
            $footer.Margin = New-Object System.Windows.Thickness(0, 0, 28, 12)
            $root.Children.Add($footer) | Out-Null

            # Measure, Arrange & Rasterize 1080x1920 PNG
            $size = New-Object System.Windows.Size(1080, 1920)
            $root.Measure($size)
            $root.Arrange((New-Object System.Windows.Rect(0, 0, 1080, 1920)))
            $root.UpdateLayout()

            $rtb = New-Object System.Windows.Media.Imaging.RenderTargetBitmap(1080, 1920, 96, 96, [System.Windows.Media.PixelFormats]::Pbgra32)
            $rtb.Render($root)

            $outDir = [System.IO.Path]::GetDirectoryName($item.outPngPath)
            if (-not [System.IO.Directory]::Exists($outDir)) {
                [System.IO.Directory]::CreateDirectory($outDir) | Out-Null
            }

            $encoder = New-Object System.Windows.Media.Imaging.PngBitmapEncoder
            $encoder.Frames.Add([System.Windows.Media.Imaging.BitmapFrame]::Create($rtb))
            $fs = [System.IO.File]::Open($item.outPngPath, [System.IO.FileMode]::Create)
            try {
                $encoder.Save($fs)
            } finally {
                $fs.Dispose()
            }
            Write-Output ("RENDERED:" + $item.locale + "/" + [System.IO.Path]::GetFileName($item.outPngPath))
        }
} catch {
    [Console]::Error.WriteLine("TOP_ERR: " + $_.Exception.ToString() + "`n" + $_.ScriptStackTrace)
    exit 4
}
