for %%i in (images\*.tar) do (
    docker load -i %%i
)
