@echo off
set ORCA=C:\Users\sakim\AppData\Local\Programs\orca\resources\bin\orca.exe
set P=C:\Users\sakim\OneDrive\Desktop\wifi
for %%T in (01 02 03 04 05 06 07 08 09) do (
  echo == T-%%T
  "%ORCA%" orchestration task-create --run run_0414b3280f3f --task-title "T-%%T" --display-name "T-%%T" --spec-file "%P%\spec-t%%T.md" --json
)
