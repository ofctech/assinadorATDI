package assinador

class UrlMappings {

    static mappings = {
        delete "/$controller/$id(.$format)?"(action: "delete")
        get "/$controller(.$format)?"(action: "index")
        get "/$controller/$id(.$format)?"(action: "show")
        post "/$controller(.$format)?"(action: "save")
        put "/$controller/$id(.$format)?"(action: "update")
        patch "/$controller/$id(.$format)?"(action: "patch")

        "/api/auth"(controller: "auth", action: "login")
        "/api/auth/verify-otp"(controller: "auth", action: "verifyOtp")
        post "/api/auth/token"(controller: "auth", action: "generateToken")

        "/api/auth"(resources: "auth")
        group "/api/auth", {
            "/token"(controller: "auth", action: "generateToken")
        }

        post "/api/signer/sign"(controller: "signer", action: "sign")
        post "/api/signer/verify"(controller: "signer", action: "verify")
        get "/api/statistics/summary"(controller: "statistics", action: "summary")
        post "/api/statistics/increment"(controller: "statistics", action: "increment")

        "/api/ldapLogin"(resources: "ldapLogin")
        group "/api/ldapLogin", {
            "/authenticate"(controller: "ldapLogin", action: "authenticate")
            "/login"(controller: "ldapLogin", action: "login")
            "/login-without-twofactor"(controller: "ldapLogin", action: "loginWithoutTwoFactor")
            "/verify-otp"(controller: "ldapLogin", action: "verifyOtp")
        }

        "/api/jwks"(resources: "jwks")
        group "/api/jwks", {
            "/idp"(controller: "jwks", action: "chavePublica")
        }

        "/api/.well-known"(resources: ".well-known")
        group "/api/.well-known", {
            "/openid-configuration"(controller: "jwks", action: "configuration")
        }

        "/"(controller: "application", action: "index")
        "500"(view: "/error")
        "404"(view: "/notFound")
    }
}
