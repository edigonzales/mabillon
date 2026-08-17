package guru.interlis.mabillon.web;

import jakarta.servlet.http.HttpServletRequest;

public final class HtmxRequest {

    private HtmxRequest() {
    }

    public static boolean isRequest(HttpServletRequest request) {
        return "true".equalsIgnoreCase(request.getHeader("HX-Request"));
    }
}
