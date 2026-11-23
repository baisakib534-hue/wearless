$ErrorActionPreference = 'Continue'
$orca = 'C:\Users\sakim\AppData\Local\Programs\orca\resources\bin\orca.exe'
$wd = 'C:\Users\sakim\OneDrive\Desktop\wifi'
$d = New-Item -ItemType Directory -Path (Join-Path $env:TEMP ([guid]::NewGuid()))
$jobs = @(
  @{ n='t06-opencode'; a='opencode'; t='task_b8f0d9bf94f5' }
)
$procs = foreach ($j in $jobs) {
  $p = Start-Process -FilePath $orca -NoNewWindow -PassThru -WorkingDirectory $wd `
    -ArgumentList "orchestration worker-start --environment local --run run_0414b3280f3f --task $($j.t) --worktree current --agent $($j.a) --keep-open --json" `
    -RedirectStandardOutput "$d\$($j.n).out" -RedirectStandardError "$d\$($j.n).err"
  $null = $p.Handle
  [pscustomobject]@{ Name=$j.n; Proc=$p }
}
foreach ($s in $procs) { $s.Proc.WaitForExit(); "$($s.Name) exit $($s.Proc.ExitCode)" }
Get-ChildItem $d | ForEach-Object { "== $($_.Name)"; Get-Content $_.FullName }
