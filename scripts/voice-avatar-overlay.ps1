param(
  [Parameter(Mandatory = $true)][string]$WavPath,
  [string]$AvatarPath = "",
  [string]$FfplayPath = "C:\Users\rodolfo\tools\ffmpeg\bin\ffplay.exe"
)

Add-Type -AssemblyName PresentationFramework
Add-Type -AssemblyName PresentationCore
Add-Type -AssemblyName WindowsBase

# Use WPF SystemParameters.WorkArea (DPI-aware DIPs, never off-screen at 125%/150% scaling)
$workArea = [System.Windows.SystemParameters]::WorkArea
$winWidth = 176
$winHeight = 204
$leftPos = [Math]::Max(16, [int]($workArea.Right - $winWidth - 20))
$topPos = [Math]::Max(16, [int]($workArea.Bottom - $winHeight - 20))

$xaml = @"
<Window xmlns="http://schemas.microsoft.com/winfx/2006/xaml/presentation"
        xmlns:x="http://schemas.microsoft.com/winfx/2006/xaml"
        WindowStyle="None"
        AllowsTransparency="True"
        Background="Transparent"
        Topmost="True"
        ShowInTaskbar="False"
        ShowActivated="False"
        ResizeMode="NoResize"
        Width="$winWidth"
        Height="$winHeight"
        Left="$leftPos"
        Top="$topPos">
  <Border CornerRadius="22" Background="#F0101014" BorderBrush="#0A84FF" BorderThickness="2.5" Margin="4">
    <Grid Margin="10">
      <Grid.RowDefinitions>
        <RowDefinition Height="*"/>
        <RowDefinition Height="Auto"/>
      </Grid.RowDefinitions>
      <Border x:Name="AvatarFrame" Grid.Row="0" Width="128" Height="128" CornerRadius="64"
              BorderBrush="#0A84FF" BorderThickness="2.5" Background="#1C1C1E" HorizontalAlignment="Center" VerticalAlignment="Center">
        <Ellipse x:Name="AvatarEllipse" Width="122" Height="122" />
      </Border>
      <StackPanel Grid.Row="1" Orientation="Horizontal" HorizontalAlignment="Center" Margin="0,8,0,0">
        <Ellipse x:Name="PulseDot" Width="8" Height="8" Fill="#0A84FF" VerticalAlignment="Center" Margin="0,0,6,0"/>
        <TextBlock x:Name="StatusText" Text="Falando..." Foreground="#F2F2F7" FontFamily="Segoe UI" FontSize="12" FontWeight="SemiBold" VerticalAlignment="Center"/>
      </StackPanel>
    </Grid>
  </Border>
</Window>
"@

$reader = New-Object System.Xml.XmlNodeReader ([xml]$xaml)
$window = [Windows.Markup.XamlReader]::Load($reader)

$avatarEllipse = $window.FindName("AvatarEllipse")
$avatarFrame = $window.FindName("AvatarFrame")
$pulseDot = $window.FindName("PulseDot")

if ($AvatarPath -and (Test-Path -LiteralPath $AvatarPath)) {
  try {
    $bitmap = New-Object System.Windows.Media.Imaging.BitmapImage
    $bitmap.BeginInit()
    $bitmap.CacheOption = [System.Windows.Media.Imaging.BitmapCacheOption]::OnLoad
    $bitmap.UriSource = New-Object System.Uri($AvatarPath)
    $bitmap.EndInit()
    $bitmap.Freeze()

    $brush = New-Object System.Windows.Media.ImageBrush
    $brush.ImageSource = $bitmap
    $brush.Stretch = [System.Windows.Media.Stretch]::UniformToFill
    $avatarEllipse.Fill = $brush
  } catch {
    $avatarEllipse.Fill = [System.Windows.Media.Brushes]::DarkSlateBlue
  }
} else {
  $avatarEllipse.Fill = [System.Windows.Media.Brushes]::DarkSlateBlue
}

# Start audio playback using ffplay (supports all WAV/PCM encodings reliably) or SoundPlayer fallback
$script:audioProc = $null
$script:soundPlayer = $null
$script:fallbackEndTime = [DateTime]::UtcNow.AddSeconds(12)

if (Test-Path -LiteralPath $FfplayPath) {
  $psi = New-Object System.Diagnostics.ProcessStartInfo
  $psi.FileName = $FfplayPath
  $psi.Arguments = "-nodisp -autoexit -loglevel quiet `"$WavPath`""
  $psi.UseShellExecute = $false
  $psi.CreateNoWindow = $true
  $script:audioProc = [System.Diagnostics.Process]::Start($psi)
} else {
  $script:soundPlayer = New-Object System.Media.SoundPlayer($WavPath)
  $script:soundPlayer.Play()
}

# Click anywhere on the avatar to stop speaking and close immediately
$window.Add_MouseLeftButtonUp({
  try { if ($script:audioProc -and -not $script:audioProc.HasExited) { $script:audioProc.Kill() } } catch {}
  try { if ($script:soundPlayer) { $script:soundPlayer.Stop() } } catch {}
  $window.Close()
})

# UI-thread timer: pulse animation + auto-close when audio finishes
$script:pulseTick = 0
$pulseTimer = New-Object System.Windows.Threading.DispatcherTimer
$pulseTimer.Interval = [TimeSpan]::FromMilliseconds(180)
$pulseTimer.Add_Tick({
  $script:pulseTick++
  $bright = ($script:pulseTick % 2 -eq 0)
  $pulseDot.Opacity = if ($bright) { 1.0 } else { 0.35 }
  $avatarFrame.BorderThickness = if ($bright) { [System.Windows.Thickness]::new(3) } else { [System.Windows.Thickness]::new(1.8) }

  if ($script:audioProc) {
    if ($script:audioProc.HasExited) {
      $pulseTimer.Stop()
      $window.Close()
    }
  } elseif ([DateTime]::UtcNow -ge $script:fallbackEndTime) {
    $pulseTimer.Stop()
    $window.Close()
  }
})
$pulseTimer.Start()

$null = $window.ShowDialog()

try { if ($script:audioProc -and -not $script:audioProc.HasExited) { $script:audioProc.Kill() } } catch {}
try { if ($script:soundPlayer) { $script:soundPlayer.Dispose() } } catch {}
try { if (Test-Path -LiteralPath $WavPath) { Remove-Item -LiteralPath $WavPath -Force -ErrorAction SilentlyContinue } } catch {}
