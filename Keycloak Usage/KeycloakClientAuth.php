<?php

namespace App\Http\Middleware;

use Closure;
use Illuminate\Http\Request;
use Symfony\Component\HttpFoundation\Response;
use Illuminate\Support\Facades\Http;

class KeycloakClientAuth
{
    /**
     * Handle an incoming request.
     *
     * @param  \Closure(\Illuminate\Http\Request): (\Symfony\Component\HttpFoundation\Response)  $next
     */
    public function handle(Request $request, Closure $next): Response
    {
        $clientId = $request->header('Client-ID');
        $clientSecret = $request->header('Client-Secret');

        // Validate Client ID and Secret
        if (!$clientId || !$clientSecret) {
            return response()->json(['error' => 'Client ID and Secret required'], 401);
        }

        // Keycloak token endpoint
        $tokenUrl = env('KEYCLOAK_URL') . '/realms/' . env('KEYCLOAK_REALM') . '/protocol/openid-connect/token';
        
        // Get token from Keycloak
        $response = Http::asForm()->post($tokenUrl, [
            'client_id' => $clientId,
            'client_secret' => $clientSecret,
            'grant_type' => 'client_credentials',
        ]);
        
        if ($response->failed()) {
            return response()->json(['error' => 'Unauthorized'], 401);
        }

        // Optionally, you can set user info in the request for later use
        $request->attributes->set('keycloak_user', $response->json());

        return $next($request);
    }
}
