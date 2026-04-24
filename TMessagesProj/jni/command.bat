@echo OFF
if exist dav1d rmdir /Q /S dav1d
if %errorlevel% neq 0 exit /b %errorlevel%
if exist dav1d exit /b 1
if %errorlevel% neq 0 exit /b %errorlevel%
call git submodule init && git submodule update
if %errorlevel% neq 0 exit /b %errorlevel%
call cd dav1d && git reset --hard HEAD && cd ..
if %errorlevel% neq 0 exit /b %errorlevel%
call export NDK=C:\Users\fre1zik\AppData\Local\Android\Sdk\ndk\21.4.7075529
if %errorlevel% neq 0 exit /b %errorlevel%
call export NINJA_PATH=`which ninja`
if %errorlevel% neq 0 exit /b %errorlevel%
call ./build_dav1d_clang.sh arm arm64
if %errorlevel% neq 0 exit /b %errorlevel%
call echo "Built archs: arm arm64"
if %errorlevel% neq 0 exit /b %errorlevel%
call 
if %errorlevel% neq 0 exit /b %errorlevel%
