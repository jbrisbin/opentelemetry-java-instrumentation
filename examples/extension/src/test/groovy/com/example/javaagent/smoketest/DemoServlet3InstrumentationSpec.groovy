package com.example.javaagent.smoketest

import io.opentelemetry.instrumentation.test.AgentInstrumentationSpecification

import javax.servlet.Filter
import javax.servlet.FilterChain
import javax.servlet.FilterConfig
import javax.servlet.ServletException
import javax.servlet.ServletOutputStream
import javax.servlet.ServletRequest
import javax.servlet.ServletResponse
import javax.servlet.http.Cookie
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletRequestWrapper
import javax.servlet.http.HttpServletResponse

class DemoServlet3InstrumentationSpec extends AgentInstrumentationSpecification {

  def "demo instrumentation adds x-server-id"() {
    setup:
    filter = new DemoFilter()
    response = new DemoServletResponse()
    filter.doFilter(null, response, null)

    assertTraces(1) {
      trace(0, 1) {
        span(0) {

        }
      }
    }
  }

  static class DemoFilter implements Filter {
    @Override
    void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {

    }

    @Override
    void destroy() {

    }
  }

  static class DemoServletResponse implements HttpServletResponse {
    final def headers = new HashMap<>()

    @Override
    void addCookie(Cookie cookie) {

    }

    @Override
    boolean containsHeader(String name) {
      return false
    }

    @Override
    String encodeURL(String url) {
      return null
    }

    @Override
    String encodeRedirectURL(String url) {
      return null
    }

    @Override
    String encodeUrl(String url) {
      return null
    }

    @Override
    String encodeRedirectUrl(String url) {
      return null
    }

    @Override
    void sendError(int sc, String msg) throws IOException {

    }

    @Override
    void sendError(int sc) throws IOException {

    }

    @Override
    void sendRedirect(String location) throws IOException {

    }

    @Override
    void setDateHeader(String name, long date) {

    }

    @Override
    void addDateHeader(String name, long date) {

    }

    @Override
    void setHeader(String name, String value) {
      headers.put(name, value)
    }

    @Override
    void addHeader(String name, String value) {

    }

    @Override
    void setIntHeader(String name, int value) {

    }

    @Override
    void addIntHeader(String name, int value) {

    }

    @Override
    void setStatus(int sc) {

    }

    @Override
    void setStatus(int sc, String sm) {

    }

    @Override
    int getStatus() {
      return 0
    }

    @Override
    String getHeader(String name) {
      return headers.get(name)
    }

    @Override
    Collection<String> getHeaders(String name) {
      return null
    }

    @Override
    Collection<String> getHeaderNames() {
      return null
    }

    @Override
    String getCharacterEncoding() {
      return null
    }

    @Override
    String getContentType() {
      return null
    }

    @Override
    ServletOutputStream getOutputStream() throws IOException {
      return null
    }

    @Override
    PrintWriter getWriter() throws IOException {
      return null
    }

    @Override
    void setCharacterEncoding(String charset) {

    }

    @Override
    void setContentLength(int len) {

    }

    @Override
    void setContentType(String type) {

    }

    @Override
    void setBufferSize(int size) {

    }

    @Override
    int getBufferSize() {
      return 0
    }

    @Override
    void flushBuffer() throws IOException {

    }

    @Override
    void resetBuffer() {

    }

    @Override
    boolean isCommitted() {
      return false
    }

    @Override
    void reset() {

    }

    @Override
    void setLocale(Locale loc) {

    }

    @Override
    Locale getLocale() {
      return null
    }
  }
}
