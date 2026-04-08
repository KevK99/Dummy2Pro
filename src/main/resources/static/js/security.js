(function () {
    const originalFetch = window.fetch.bind(window);

    function getCookie(name) {
        const prefix = `${name}=`;
        return document.cookie
            .split(";")
            .map(entry => entry.trim())
            .find(entry => entry.startsWith(prefix))
            ?.substring(prefix.length) ?? "";
    }

    function isUnsafeMethod(method) {
        return !["GET", "HEAD", "OPTIONS", "TRACE"].includes(String(method || "GET").toUpperCase());
    }

    async function refreshCsrfToken() {
        const response = await originalFetch("/csrf", {
            method: "GET",
            credentials: "same-origin"
        });

        if (!response.ok) {
            throw new Error("CSRF-Token konnte nicht geladen werden.");
        }

        return getCookie("XSRF-TOKEN");
    }

    async function fetchWithCsrfRetry(input, init = {}, alreadyRetried = false) {
        const options = { ...init };
        const method = String(options.method || "GET").toUpperCase();
        const url = typeof input === "string" ? input : (input?.url ?? "");
        const isSameOrigin = url.startsWith("/") || url.startsWith(window.location.origin);

        if (isSameOrigin) {
            options.credentials = options.credentials || "same-origin";

            if (isUnsafeMethod(method)) {
                const headers = new Headers(options.headers || {});
                let csrfToken = getCookie("XSRF-TOKEN");

                if (!csrfToken && url !== "/api/login" && url !== "/api/register") {
                    csrfToken = await refreshCsrfToken();
                }

                if (csrfToken && !headers.has("X-XSRF-TOKEN")) {
                    headers.set("X-XSRF-TOKEN", csrfToken);
                }

                options.headers = headers;
            }
        }

        const response = await originalFetch(input, options);

        if (
            response.status === 403 &&
            !alreadyRetried &&
            isSameOrigin &&
            isUnsafeMethod(method) &&
            url !== "/api/login" &&
            url !== "/api/register" &&
            url !== "/csrf"
        ) {
            await refreshCsrfToken();

            const retryOptions = { ...init };
            const retryHeaders = new Headers(retryOptions.headers || {});
            const freshToken = getCookie("XSRF-TOKEN");

            if (freshToken && !retryHeaders.has("X-XSRF-TOKEN")) {
                retryHeaders.set("X-XSRF-TOKEN", freshToken);
            }

            retryOptions.headers = retryHeaders;
            retryOptions.credentials = retryOptions.credentials || "same-origin";

            return fetchWithCsrfRetry(input, retryOptions, true);
        }

        return response;
    }

    window.Dummy2ProSecurity = {
        async syncCurrentUser() {
            const response = await originalFetch("/api/user/me", {
                credentials: "same-origin"
            });

            if (!response.ok) {
                return null;
            }

            const data = await response.json();
            sessionStorage.setItem("userId", String(data.userId));
            sessionStorage.setItem("username", data.username || "");
            sessionStorage.setItem("avatar", data.avatar || "duck.jpg");
            return data;
        },

        async ensureCsrfToken() {
            let token = getCookie("XSRF-TOKEN");

            if (!token) {
                token = await refreshCsrfToken();
            }

            return token;
        },

        async refreshCsrfToken() {
            return refreshCsrfToken();
        }
    };

    window.fetch = function (input, init = {}) {
        return fetchWithCsrfRetry(input, init, false);
    };
})();