<?php

namespace App\Http\Middleware;

use Closure;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Auth;
use Illuminate\Support\Facades\Config;
use Illuminate\Support\Facades\Redis;
use Symfony\Component\HttpFoundation\Response;
use App\Services\KeycloakService;

class NewGeneratedToken
{
    /**
     * Handle an incoming request.
     *
     * @param  \Closure(\Illuminate\Http\Request): (\Symfony\Component\HttpFoundation\Response)  $next
     */
    public function handle(Request $request, Closure $next): Response
    {
        $service = new KeycloakService();
        //set current user on session
        $user = json_decode(Auth::token(), true);
        session(['user_info' => [
            'id' => $user['sub'],
            'username' => $user['preferred_username'],
            'roles' => $user['resource_access'],
            'realm_roles' => $user['realm_access'],
            'token' => $request->bearerToken()
        ]]);
        //check token be
        $existedToken = Redis::get(Config::get('app.env').'_secretTokenUserManagementBe');
        if ($existedToken) {
            $res = $service->tokenDecode($request);
            $newToken = $existedToken;
            if (isset($res['error'])) {
                $newToken = $service->token();
                if (isset($newToken[0]['error'])) {
                    return response()->json([
                        'status' => 401,
                        'timestamp' => new \DateTime(),
                        'path' => $request->url(),
                        "error" => "Token backend gagal digenerate (".$newToken[0]['error'].")"
                    ], 401);
                }
                Redis::set(Config::get('app.env').'_secretTokenUserManagementBe', $newToken);
            }
        } else {
            $newToken = $service->token();
            if (isset($newToken[0]['error'])) {
                return response()->json([
                    'status' => 401,
                    'timestamp' => new \DateTime(),
                    'path' => $request->url(),
                    "error" => "Token backend gagal digenerate (".$newToken[0]['error'].")"
                ], 401);
            }
            \Log::info($newToken);
            $newToken = $newToken[0]['access_token'];
        }
        $request->headers->set('Authorization', 'Bearer '.$newToken);
        return $next($request);
    }
}
