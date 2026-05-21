package com.sysu.edu.browser;

public record BrowserCookieManager() {
    
    static final String clearAllCookies = """
                                (function clearAllCookies() {
                            let cookies = document.cookie.split(";");
                            cookies.forEach(cookie => {
                                    let cookieName = cookie.split("=")[0].trim();
                            document.cookie = cookieName + "=; expires=Thu, 01 Jan 1970 00:00:00 GMT";
                                    });
            console.log(document.cookie);
                        })();
            """;
}
