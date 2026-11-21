@echo off
title Antigravity - Midiastore
cd /d "C:\Users\rodolfo\midiastory-"

REM Inicia o painel flutuante de avatar e voz em segundo plano (sem janela extra)
start "" /b powershell.exe -NoProfile -STA -WindowStyle Hidden -ExecutionPolicy Bypass -File "C:\Users\rodolfo\midiastory-\scripts\antigravity-companion.ps1"

REM Inicia o Antigravity CLI ja dentro da pasta correta do projeto Midiastore
"C:\Users\rodolfo\AppData\Local\agy\bin\agy.exe"
