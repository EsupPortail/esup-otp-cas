<%--
    Licensed to Jasig under one or more contributor license
    agreements. See the NOTICE file distributed with this work
    for additional information regarding copyright ownership.
    Jasig licenses this file to you under the Apache License,
    Version 2.0 (the "License"); you may not use this file
    except in compliance with the License.  You may obtain a
    copy of the License at the following location:
      http://www.apache.org/licenses/LICENSE-2.0
    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on an
    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied.  See the License for the
    specific language governing permissions and limitations
    under the License.
--%>
<jsp:directive.include file="includes/top.jsp" />
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"%>
<c:if test="${not pageContext.request.secure}">
	<div id="msg" class="alert alert-warning">
		<h2>Non-secure Connection</h2>
		<p>You are currently accessing CAS over a non-secure connection.
			Single Sign On WILL NOT WORK. In order to have single sign on work,
			you MUST log in over HTTPS.</p>
	</div>
</c:if>
<div id="msg2" hidden="true"></div>
<div class="col-sm-6">
	<div class="form-box" id="login">
		<form:form method="post" id="fm1" commandName="${commandName}"
			htmlEscape="true">
			<div class="form-top">
				<div class="form-top-left">
					<form:errors path="*" id="msg" cssClass="alert alert-danger"
						element="div" htmlEscape="false">
						<a href="#" class="close" data-dismiss="alert" aria-label="close"
							title="close">×</a>
					</form:errors>
					<h3 id="instructions_username">
						<spring:message code="screen.instructions.username" />
					</h3>
					<h3 id="instructions_transport" hidden="true">
						<spring:message code="screen.instructions.transport" />
					</h3>
					<h3 id="instructions_code" hidden="true">
						<spring:message code="screen.instructions.code" />
					</h3>
				</div>
				<div class="form-top-right">
					<i class="fa fa-lock"></i>
				</div>
			</div>
			<div class="form-bottom">
				<!-- <label for="username" style="display: inline-block;"><spring:message
								code="screen.welcome.label.netid" /></label> -->
				<c:if test="${not empty header['REMOTE_USER']}">
					<c:set var='varRemoteUser' value="${header['REMOTE_USER']}" />
				</c:if>
				<jsp:useBean id="esupOtpApiAuthenticationHandler"
					class="org.esupportail.cas.authentication.EsupOtpApiAuthenticationHandler" />
				<c:choose>
					<c:when test="${not empty sessionScope.openIdLocalId}">
						<strong>${sessionScope.openIdLocalId}</strong>
						<input type="hidden" id="username" name="username"
							value="${sessionScope.openIdLocalId}" />
						<%
							String openIdLocalId = session.getAttribute("openIdLocalId").toString();
						%>
						<input type="hidden" id="userhash"
							value='<%=esupOtpApiAuthenticationHandler.getUserHash(openIdLocalId)%>' />
						<div class="form-group">
							<label id='usernameLabel'">${sessionScope.openIdLocalId}</label>
						</div>
						<script type="text/javascript">
							function getUserHash() {
								return document.getElementById('userhash').value;
							}
						</script>
					</c:when>
					<c:when test="${not empty varRemoteUser}">
						<strong>${sessionScope.openIdLocalId}</strong>
						<input type="hidden" id="username" name="username"
							value="${varRemoteUser}" />
						<%
							String remoteUser =request.getHeader("REMOTE_USER"); 
						%>
						<input type="hidden" id="userhash"
							value="<%=esupOtpApiAuthenticationHandler.getUserHash(remoteUser)%>" />
						<div class="form-group">
							<label id='usernameLabel'>${varRemoteUser}</label>
						</div>
						<script type="text/javascript">
							function getUserHash() {
								return document.getElementById('userhash').value;
							}
						</script>
					</c:when>
					<c:otherwise>
						<spring:message code="screen.welcome.label.netid.accesskey"
							var="userNameAccessKey" />
						<div class="form-group">
							<label id='usernameLabel'></label>
						</div>
						<div class="form-group">
							<input type="text" name="username"
								placeholder="<spring:message
								code='error.login_needed'/>..."
								class="form-username form-control" id="username" size="25"
								tabindex="1" accesskey="${userNameAccessKey}" path="username"
								autocomplete="off" htmlEscape="true">
						</div>
						<script type="text/javascript">
							var users_secret = "<jsp:getProperty name='esupOtpApiAuthenticationHandler' property='usersSecret' />";
							function getUserHash() {
								var d = new Date();
								var uid = document.getElementById('username').value;
								var salt = d.getUTCDate().toString()
										+ d.getUTCHours().toString();
								return CryptoJS.SHA256(
										CryptoJS.MD5(users_secret).toString()
												+ uid + salt).toString();
							}
						</script>
					</c:otherwise>
				</c:choose>
				<!-- <input type="button" class="button"
						value="<spring:message code='button.change'/>   &#xf044;"
						id="resetUsername" onclick="reset_username();"> -->
				<input type="button" class="button"
					value="<spring:message code='button.validate'/>  &#xf058;"
					id="buttonMethods" onclick="get_user_auth();">
				<div id="list-methods"></div>
				<div id="auth">
					<%--
            NOTE: Certain browsers will offer the option of caching passwords for a user.  There is a non-standard attribute,
            "autocomplete" that when set to "off" will tell certain browsers not to prompt to cache credentials.  For more
            information, see the following web page:
            http://www.technofundo.com/tech/web/ie_autocomplete.html
            --%>
					<div class="form-group">
						<form:password cssClass="required form-password form-control" cssErrorClass="error" id="password" size="25" tabindex="2" path="password"  accesskey="${passwordAccessKey}" htmlEscape="true" autocomplete="off" />
					</div>
				</div>
			</div>
			<div class="social-login" id="own-code" hidden="true">
				<input id="ownCodeInput" class="button ok" style=""
					position:relative !important;" type="button"
					value="<spring:message code='button.code.owned'/>  &#xf084;"
					onclick='hide_methods();'>
			</div>
			<div id="auth-option" hidden="true">
					<input id="warn" name="warn" value="true" tabindex="3"
						accesskey="<spring:message code="screen.welcome.label.warn.accesskey" />"
						type="checkbox" /> <label for="warn"><spring:message
							code="screen.welcome.label.warn" /></label>
							<br>
					<input type="checkbox" name="rememberMe" id="rememberMe"
						value="true" /> <label for="rememberMe">Remember Me</label> <input
						type="hidden" name="lt" value="${loginTicket}" /> <input
						type="hidden" name="execution" value="${flowExecutionKey}" /> <input
						type="hidden" name="_eventId" value="submit" />
				<p>
					<input id="submit" class="btn-submit button" name="submit"
						accesskey="l"
						value="<spring:message code="screen.welcome.button.login" />"
						tabindex="4" type="" />
				</p>
			</div>
			<div class="social-login" id="lost-code" hidden="true">
				<input type='button' class="error-button"
					value="<spring:message code='button.code.lost'/>  &#xf128;"
					onclick="show_methods();">
			</div>
		</form:form>
	</div>
	<div class="social-login">
		<h3></h3>
		<div class="social-login-buttons"></div>
	</div>
</div>
<div class="col-sm-6">
	<div class="form-box">
		<div class="form-top">
			<div class="form-top-left">
				<p>
					<spring:message code="screen.welcome.security" />
				</p>
			</div>
			<div class="form-top-right">
				<i class="fa fa-exclamation-triangle"></i>
			</div>
		</div>
		<div class="form-bottom">
			<div id="list-languages">
				<%
					final String queryString = request.getQueryString() == null ? ""
							: request.getQueryString().replaceAll(
									"&locale=([A-Za-z][A-Za-z]_)?[A-Za-z][A-Za-z]|^locale=([A-Za-z][A-Za-z]_)?[A-Za-z][A-Za-z]",
									"");
				%>
				<c:set var='query' value='<%=queryString%>' />
				<c:set var="xquery" value="${fn:escapeXml(query)}" />

				<h3>Languages:</h3>

				<c:choose>
					<c:when
						test="${not empty requestScope['isMobile'] and not empty mobileCss}">
						<form method="get" action="login?${xquery}">
							<select name="locale">
								<option value="en">English</option>
								<option value="es">Spanish</option>
								<option value="fr">French</option>
								<option value="ru">Russian</option>
								<option value="nl">Nederlands</option>
								<option value="sv">Svenska</option>
								<option value="it">Italiano</option>
								<option value="ur">Urdu</option>
								<option value="zh_CN">Chinese (Simplified)</option>
								<option value="zh_TW">Chinese (Traditional)</option>
								<option value="de">Deutsch</option>
								<option value="ja">Japanese</option>
								<option value="hr">Croatian</option>
								<option value="cs">Czech</option>
								<option value="sl">Slovenian</option>
								<option value="pl">Polish</option>
								<option value="ca">Catalan</option>
								<option value="mk">Macedonian</option>
								<option value="fa">Farsi</option>
								<option value="ar">Arabic</option>
								<option value="pt_PT">Portuguese</option>
								<option value="pt_BR">Portuguese (Brazil)</option>
							</select> <input type="submit" value="Switch">
						</form>
					</c:when>
					<c:otherwise>
						<c:set var="loginUrl"
							value="login?${xquery}${not empty xquery ? '&' : ''}locale=" />
						<ul>
							<li class="first"><a href="${loginUrl}en">English</a></li>
							<li><a href="${loginUrl}es">Spanish</a></li>
							<li><a href="${loginUrl}fr">French</a></li>
							<li><a href="${loginUrl}ru">Russian</a></li>
							<li><a href="${loginUrl}nl">Nederlands</a></li>
							<li><a href="${loginUrl}sv">Svenska</a></li>
							<li><a href="${loginUrl}it">Italiano</a></li>
							<li><a href="${loginUrl}ur">Urdu</a></li>
							<li><a href="${loginUrl}zh_CN">Chinese (Simplified)</a></li>
							<li><a href="${loginUrl}zh_TW">Chinese (Traditional)</a></li>
							<li><a href="${loginUrl}de">Deutsch</a></li>
							<li><a href="${loginUrl}ja">Japanese</a></li>
							<li><a href="${loginUrl}hr">Croatian</a></li>
							<li><a href="${loginUrl}cs">Czech</a></li>
							<li><a href="${loginUrl}sl">Slovenian</a></li>
							<li><a href="${loginUrl}ca">Catalan</a></li>
							<li><a href="${loginUrl}mk">Macedonian</a></li>
							<li><a href="${loginUrl}fa">Farsi</a></li>
							<li><a href="${loginUrl}ar">Arabic</a></li>
							<li><a href="${loginUrl}pt_PT">Portuguese</a></li>
							<li><a href="${loginUrl}pt_BR">Portuguese (Brazil)</a></li>
							<li class="last"><a href="${loginUrl}pl">Polish</a></li>
						</ul>
					</c:otherwise>
				</c:choose>
			</div>
		</div>
	</div>
</div>

<jsp:directive.include file="includes/bottom.jsp" />
