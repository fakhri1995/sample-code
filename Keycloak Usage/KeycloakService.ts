import Keycloak from 'keycloak-js';
import ApiService from "@/core/services/ApiService";

const initkeycloakOption = {
    url: import.meta.env.VITE_APP_SSO_URL,
    realm: import.meta.env.VITE_APP_SSO_REALM,
    clientId: import.meta.env.VITE_APP_SSO_CLIENT_ID,
}

var keycloak = new Keycloak(initkeycloakOption)

const  initKeycloak = (): Promise<boolean|void> => {
    console.log('init keycloak');
    return keycloak.init({
        onLoad: 'check-sso',
        checkLoginIframe: false,
        // silentCheckSsoRedirectUri: window.location.origin + "/silent-check-sso",
    })
    .then(authenticated => {
        console.log(`User is ${authenticated ? 'authenticated' : 'not authenticated'}`);
        if (authenticated) {
            // window.location.href =  window.location.origin+"/dist/dashboard";
            ApiService.setHeader();
        }
        return authenticated;
    })
    .catch(error => {
        console.log('Failed to initialize adapter:', error);
        console.error('Failed to initialize adapter:', error);
    });
}

const loginKeycloak = async (): Promise<void> => {
    console.log('login keycloak', isAuthenticated());

    if (!isAuthenticated()) {
        console.log('User needs to login');
        await keycloak.login({redirectUri: import.meta.env.VITE_APP_LANDING_PAGE_URL + '/home/portal'});
    } else {
        console.log('User is already logged in');
    }
}

const logoutKeycloak = (): Promise<void> => {
    // keycloak.createLogoutUrl = function(options?: any): string {
    //     return keycloak.endpoints.logout();
    // };
    return keycloak.logout({ redirectUri: window.origin  });
}


const getKeycloakData = (dataKey: string): any => {
    return JSON.parse(localStorage.getItem('id_token')!)[dataKey];
}

const getKeycloakResourceAccess = () => {
    console.log("get keycloak resource access", keycloak.resourceAccess)
    return keycloak.resourceAccess;
}

const getTokenKeycloak = () => {
    return keycloak.token
}

const getTokenParsed = () => {return keycloak.tokenParsed}

const updateToken = async (): Promise<void> => {
    try {
        await keycloak.updateToken(60);
        ApiService.setHeader();
    } catch (error) {
        console.error('Failed to update token:', error);
    }
}

const getKeycloak = (): Keycloak => {
    console.log("====< keycloak >====");
    console.log(keycloak);
    console.log("====================");
    return keycloak;
}

const isAuthenticated = (): boolean|undefined  => {
    console.log("isauthenticated? ", keycloak.authenticated)
    return keycloak.authenticated;
}

export default keycloak;
export {
    loginKeycloak,
    logoutKeycloak,
    updateToken,
    getKeycloakData,
    initKeycloak,
    keycloak,
    isAuthenticated,
    getKeycloakResourceAccess,
    getTokenKeycloak,
    getKeycloak,
    getTokenParsed 
};