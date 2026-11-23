@echo off
set P=C:\Users\sakim\OneDrive\Desktop\wifi
git -C "%P%" init -q
git -C "%P%" config user.name "goose"
git -C "%P%" config user.email "goose@local"
git -C "%P%" add .gitignore TOOLING-PLAN.md TODO.md WEARLESS-BOOK.md BUILD-ROADMAP.md WEARLESS-BUILD-PROMPT.md "Wearless Deep Research Report.html" specs-*.md
git -C "%P%" commit -q -m "plan: binding tooling plan and task board"
git -C "%P%" log -1 --oneline
