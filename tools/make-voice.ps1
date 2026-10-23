# Records the 12 spoken lines in tools/voice-lines.txt (the reminder before each prayer and "its time has begun")
# into app/src/main/assets/announce/<name>.wav with Windows' own Arabic voice (OneCore, e.g. Microsoft Naayf), for
# Announce.java. They are made on your PC and never committed (.gitignore): the voice's licence covers your own
# use, not redistribution. Without them the reminders still show, silently.
# Needs Windows 10/11 with an Arabic speech voice (Settings > Time & language > Speech > Add voices).
# Run from anywhere: powershell -ExecutionPolicy Bypass -File tools\make-voice.ps1
param([string]$Lines = (Join-Path $PSScriptRoot 'voice-lines.txt'))
$dest = Join-Path (Split-Path $PSScriptRoot -Parent) 'app\src\main\assets\announce'
New-Item -ItemType Directory -Force $dest | Out-Null
Add-Type -AssemblyName System.Runtime.WindowsRuntime
$null = [Windows.Media.SpeechSynthesis.SpeechSynthesizer, Windows.Media.SpeechSynthesis, ContentType = WindowsRuntime]
$asTask = ([System.WindowsRuntimeSystemExtensions].GetMethods() | Where-Object {
    $_.Name -eq 'AsTask' -and $_.GetParameters().Count -eq 1 -and $_.GetParameters()[0].ParameterType.Name -eq 'IAsyncOperation`1' })[0]
function Await($op, [type]$type) {
    $t = $asTask.MakeGenericMethod($type).Invoke($null, @($op))
    $t.Wait(-1) | Out-Null
    $t.Result
}
$synth = New-Object Windows.Media.SpeechSynthesis.SpeechSynthesizer
$voice = [Windows.Media.SpeechSynthesis.SpeechSynthesizer]::AllVoices | Where-Object { $_.Language -like 'ar*' } | Select-Object -First 1
if (-not $voice) { throw 'No Arabic voice installed' }
$synth.Voice = $voice
"voice: $($voice.DisplayName) ($($voice.Language))"
foreach ($line in Get-Content -Encoding UTF8 $Lines) {
    if (-not $line.Trim()) { continue }
    $name, $text = $line -split '\|', 2
    $stream = Await ($synth.SynthesizeTextToStreamAsync($text)) ([Windows.Media.SpeechSynthesis.SpeechSynthesisStream])
    $out = Join-Path $dest "$name.wav"
    $src = [System.IO.WindowsRuntimeStreamExtensions]::AsStreamForRead($stream)
    $dst = [System.IO.File]::Create($out)
    $src.CopyTo($dst)
    $dst.Close(); $src.Close()
    "{0} ({1:N0} KB)" -f (Split-Path $out -Leaf), ((Get-Item $out).Length / 1KB)
}
