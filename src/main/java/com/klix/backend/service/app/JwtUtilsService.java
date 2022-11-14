package com.klix.backend.service.app;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.klix.backend.model.User;
import com.klix.backend.service.user.UserService;
import com.klix.backend.service.interfaces.SecurityService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.SignatureException;
import io.jsonwebtoken.UnsupportedJwtException;
import lombok.extern.slf4j.Slf4j;


/**
 * TODO: Kommentar
 */
@Slf4j
@Service
public class JwtUtilsService
{
    private final String HEADER = "Authorization";
	private final String PREFIX = "Bearer ";    
    private final String SECRET = "mySecretKey";

    private final int DAY_IN_MILLISECONDS = 1000 * 60 * 60 * 24;

    @Autowired private SecurityService securityService;
    @Autowired private UserService userService;


    /**
     * Generate token and parse claims
     */
	public String generateJWTToken(String username)
	{
		List<GrantedAuthority> grantedAuthorities = AuthorityUtils.commaSeparatedStringToAuthorityList("ROLE_USER");
		
		String token = Jwts.builder()
							.setId("softtekJWT")
							.setSubject(username)
							.claim("authorities", grantedAuthorities.stream().map(GrantedAuthority::getAuthority)
																			 .collect(Collectors.toList()))
							.setIssuedAt(new Date(System.currentTimeMillis()))

							// Set token expiration, currently 1 day:
							.setExpiration(new Date(System.currentTimeMillis() + DAY_IN_MILLISECONDS)) 
							.signWith(SignatureAlgorithm.HS512, SECRET.getBytes()).compact();

		return this.PREFIX + token;
	}


    /**
     * 
     */
    public boolean checkRequest(final HttpServletRequest request, HttpServletResponse response)
    {
        log.debug("doFilterInternal start");
        final String token = request.getHeader(HEADER);

        if (!this.isTokenValid(token))
        {
            log.debug("checkJWTToken false");
            SecurityContextHolder.clearContext();
            return false;
        }

        log.debug("checkJWTToken true");

        try
        {
            try
            {
                Claims claims = extractAllClaims(token);
                if (claims.get("authorities") == null)
                {
                    SecurityContextHolder.clearContext();
                    return false;
                }

                setUpSpringAuthentication(claims);
                return true;
    
            }
            catch (ExpiredJwtException | UnsupportedJwtException | MalformedJwtException e)
            {
                // kann das in den Endpunkten vonm Controller verwendet werden?
                // Wenn nicht brauchen wir die Info über die JWT Fehler

                log.error(e.getMessage());
                response.sendError(HttpServletResponse.SC_FORBIDDEN, e.getMessage());
                return false;
            }
        }
        catch(IOException e)
        {
            log.error(e.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return false;
        }
	}

    
	/**
	 * Authentication method in Spring flow
	 * 
	 * @param claims
	 */
	private void setUpSpringAuthentication(Claims claims)
	{
		log.debug("setUpSpringAuthentication start");

		@SuppressWarnings("unchecked")
		List<String> authorities = (List<String>) claims.get("authorities");

		UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
			claims.getSubject(),
			null,
			authorities.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList()));

		SecurityContextHolder.getContext().setAuthentication(auth);
	}


    /**
     * returns wether the passed token is syntactically correct and not expired
     */
	private boolean isTokenValid(String token)
	{
		log.debug("checkJWTToken start");

        try {
            return token != null && token.startsWith(PREFIX) && !this.isTokenExpired(token);
        } catch (SignatureException e) { return false; }
    }


    /**
     * gets the user from a jwt-token of header named 'Authentication' of passed request.
     * Sends potential errors directly via response.
     * 
     * !TODO need to get errors to controller
     */
    public User getUser(final HttpServletRequest request, final HttpServletResponse response)
        throws IOException
    {
        // get current user
        final String token = request.getHeader("Authentication");

        String username = null;
        User user = null;
        if (token == null)
        {
            user = this.securityService.findLoggedInUser();
        } else
        {
            try {
                username = this.extractUsername(token);
                user = this.userService.findByUsername( username );

            } catch (SignatureException e) {
                log.warn("in getUser(): jwt-token is not trustworthy. Exit.");
                //response.setStatus(HttpServletResponse.SC_FORBIDDEN, "jwt-token is not trustworthy");
                return null;

            } catch (ExpiredJwtException e) {
                log.warn("in getUser(): jwt-token is expired. Exit.");
                //response.setStatus(HttpServletResponse.SC_UNAUTHORIZED, "jwt-token is expired");
                return null;
            }
        }

        if (user == null)
        {
            if (username == null) username = "NO USERNAME FOUND";

            // not authenticated or not known at all
            log.warn("in getUser(): User with name " + username + " could not be found.");
            //response.setStatus(HttpServletResponse.SC_BAD_REQUEST, "User could not be found");
            return null;
        }

        return user;
    }


    /**
     * Removes this.PREFIX from token, since it was added after generating a token in the first place.
     * Needed to process the token further, e.g. extracting claims.
     * 
     * @see {@link JwtUtils.generateJWTToken}
     */
    private String removeTokenPrefix(String token)
    {
        return token.replace(this.PREFIX, "");
    }

    // private boolean validateToken(String token, UserDetails userDetails)
    // {
    //     final String username = this.extractUsername(token);
    //     return username.equals(userDetails.getUsername()) && !this.isTokenExpired(token);
    // }

    /**
     * Compares extracted expiration date from token with the current date.
     * Throws a io.jsonwebtoken.SignatureException if the token given as input is not trustworthy.
     */
    private boolean isTokenExpired(String token) throws SignatureException
    {
        try {
            return this.extractExpiration(token).before(new Date());
        } catch (ExpiredJwtException e) { return true; }
    }


    /**
     * retrieves username from given token.
     * Throws a io.jsonwebtoken.SignatureException if the token given as input is not trustworthy.
     * Throws a io.jsonwebtoken.ExpiredJwtException if the token given as input is expired.
     */
    public String extractUsername(String token) throws SignatureException, ExpiredJwtException
    {
        return this.extractClaim(token, Claims::getSubject);
    }


    /**
     * retrieves the expiration date from given token.
     * Throws a io.jsonwebtoken.SignatureException if the token given as input is not trustworthy.
     * Throws a io.jsonwebtoken.ExpiredJwtException if the token given as input is expired.
     */
    public Date extractExpiration(String token) throws SignatureException, ExpiredJwtException
    {
        return this.extractClaim(token, Claims::getExpiration);
    }


    /**
     * retrieves claim of token with given resolver.
     * Throws a io.jsonwebtoken.SignatureException if the token given as input is not trustworthy.
     * Throws a io.jsonwebtoken.ExpiredJwtException if the token given as input is expired.
     */
    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) throws SignatureException, ExpiredJwtException
    {
        final Claims claims = this.extractAllClaims(token);
        return claimsResolver.apply(claims);
    }


    /**
     * retrieves all claims of token.
     * Throws a io.jsonwebtoken.SignatureException if the token given as input is not trustworthy.
     * Throws a io.jsonwebtoken.ExpiredJwtException if the token given as input is expired.
     */
    private Claims extractAllClaims(String token) throws SignatureException, ExpiredJwtException
    {
        final String cleanedToken = this.removeTokenPrefix(token);
        return Jwts.parser().setSigningKey(SECRET.getBytes()).parseClaimsJws(cleanedToken).getBody();
    }
}