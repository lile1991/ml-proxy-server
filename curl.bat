# --insecure 跳过https请求证书校验
curl --insecure -v -x http://127.0.0.1:40000 https://ipinfo.io

# --proxy-insecure 跳过https代理证书校验
curl --proxy-insecure -v -x  https://127.0.0.1:40000 https://ipinfo.io -k

# socks5代理
curl -v -x  socks5://auh:123123@127.0.0.1:40000 http://ipinfo.io


# 中继代理
curl -v -x  http://auh:456789@127.0.0.1:40001 https://ipinfo.io -k
curl -v -x  http://auh:456789@127.0.0.1:40001 http://ipinfo.io -k
