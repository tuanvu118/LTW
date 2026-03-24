package com.BTL_JAVA.BTL.Service;

import com.BTL_JAVA.BTL.DTO.Request.Auth.*;
import com.BTL_JAVA.BTL.DTO.Response.Auth.AuthenticationResponse;
import com.BTL_JAVA.BTL.DTO.Response.Auth.IntrospectResponse;
import com.BTL_JAVA.BTL.Entity.InvalidtedToken;
import com.BTL_JAVA.BTL.Entity.Role;
import com.BTL_JAVA.BTL.Entity.User;
import com.BTL_JAVA.BTL.Exception.AppException;
import com.BTL_JAVA.BTL.Exception.ErrorCode;
import com.BTL_JAVA.BTL.Repository.InvalidtedTokenRepository;
import com.BTL_JAVA.BTL.Repository.httpclient.OutboundIdentityClient;
import com.BTL_JAVA.BTL.Repository.UserRepository;
import com.BTL_JAVA.BTL.Repository.httpclient.OutboundUserClient;
import com.BTL_JAVA.BTL.Repository.RoleRepository;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.text.ParseException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthenticationService {
    UserRepository userRepository;
    OutboundIdentityClient outboundIdentityClient;
    InvalidtedTokenRepository invalidtedTokenRepository;
    OutboundUserClient outboundUserClient;
    RoleRepository roleRepository;

    @NonFinal
    @Value("${jwt.signerKey}")
    protected  String SIGNER_KEY;

    @NonFinal
    @Value("${jwt.valid-duration}")
    protected  long VALIDATION_DURATION;

    @NonFinal
    @Value("${jwt.refresh-duration}")
    protected  long REFRESH_DURATION;

    @NonFinal
    @Value("${outbound.identity.google.client-id}")
    protected String CLIENT_ID;

    @NonFinal
    @Value("${outbound.identity.google.client-secret}")
    protected String CLIENT_SECRET;

    @NonFinal
    @Value("${outbound.identity.redirect-uri}")
    protected String REDIRECT_URI;

    @NonFinal
    protected String GRANT_TYPE = "authorization_code";

    public IntrospectResponse introspect(IntrospectRequest request) throws JOSEException, ParseException {
        var token=request.getToken();
        boolean isValid=true;
        SignedJWT signedJWT=null;

       try {
           signedJWT= verifyToken(token,false);
       }catch (AppException | JOSEException|ParseException e){
        isValid=false;
       }
        return IntrospectResponse.builder()
                .valid(isValid)
                .userId(Objects.nonNull(signedJWT)? signedJWT.getJWTClaimsSet().getSubject():null)
                .build();
    }

    public AuthenticationResponse outboundAuthenticate(String code) {
        var response = outboundIdentityClient.exchangeToken(ExchangeTokenRequest.builder()
                .code(code)
                .clientId(CLIENT_ID)
                .clientSecret(CLIENT_SECRET)
                .redirectUri(REDIRECT_URI)
                .grantType(GRANT_TYPE)
                .build());
        log.info("GOOGLE TOKEN RESPONSE {}", response);


        var userInfo = outboundUserClient.getUserInfo("json", response.getAccessToken());


        // Tìm role USER đã tồn tại trong DB
        Set<Role> roles = new HashSet<>();
        Role userRole = roleRepository.findById(com.BTL_JAVA.BTL.enums.Role.USER.toString())
                .orElseThrow(() -> {
                    log.error("Role USER not found in database");
                    return new AppException(ErrorCode.UNCATEGORIED_EXCEPTION);
                });
        roles.add(userRole);

        // Tìm user theo EMAIL (duy nhất) thay vì fullName (có thể trùng)
        var user = userRepository.findByEmail(userInfo.getEmail())
                .orElseGet(() -> userRepository.save(User.builder()
                                .fullName(userInfo.getName())
                                .email(userInfo.getEmail())
                                .roles(roles)
                        .build())
                );

        var token= generateToken(user);

        return AuthenticationResponse.builder()
                .token(token)
                .authenticated(true)
                .build();
    }

   public AuthenticationResponse authenticated(AuthenticationRequest request) {

        log.info("SIGNKEY: {}",SIGNER_KEY);
          var user = userRepository.findByFullName(request.getFullName())
                  .orElseThrow(() ->new AppException(ErrorCode.USER_NOT_EXISTED));

        PasswordEncoder  passwordEncoder = new BCryptPasswordEncoder(10);
        boolean authenticate= passwordEncoder.matches(request.getPassword(), user.getPassword());
        if(!authenticate){
            throw new AppException(ErrorCode.USER_NOT_EXISTED);
        }
        var token= generateToken(user);

        return AuthenticationResponse.builder()
                .token(token)
                .authenticated(true)
                .build();

    }

    public void logout(LogoutRequest request) throws ParseException, JOSEException {
        try {
            var signToken = verifyToken(request.getToken(), true);

            String jit=signToken.getJWTClaimsSet().getJWTID();
            Date expiryTime=signToken.getJWTClaimsSet().getExpirationTime();

            InvalidtedToken invalidtedToken=InvalidtedToken.builder()
                    .id(jit)
                    .expiryTime(expiryTime)
                    .build();
            invalidtedTokenRepository.save(invalidtedToken);
        }catch (AppException e){
            log.info("Token already expired");
        }

    }

    public AuthenticationResponse refreshToken(RefreshRequest request) throws ParseException, JOSEException {

        var signToken = verifyToken(request.getToken(),true);

        String jit=signToken.getJWTClaimsSet().getJWTID();
        Date expiryTime=signToken.getJWTClaimsSet().getExpirationTime();

        InvalidtedToken invalidtedToken=InvalidtedToken.builder()
                .id(jit)
                .expiryTime(expiryTime)
                .build();
        invalidtedTokenRepository.save(invalidtedToken);


        var userId=signToken.getJWTClaimsSet().getSubject();

        var user=userRepository.findById(Integer.parseInt(userId)).orElseThrow(() ->new AppException(ErrorCode.UNAUTHENTICATED));

        var token= generateToken(user);

        return AuthenticationResponse.builder()
                .token(token)
                .authenticated(true)
                .build();

    }

    private SignedJWT verifyToken(String token,boolean isRefrseh) throws JOSEException, ParseException {
        JWSVerifier verifier= new MACVerifier(SIGNER_KEY.getBytes());

        SignedJWT signedJWT=SignedJWT.parse(token);

        Date expityTime=(isRefrseh)
                ?new Date(signedJWT.getJWTClaimsSet().getIssueTime().toInstant().plus(REFRESH_DURATION,ChronoUnit.SECONDS).toEpochMilli())
                :signedJWT.getJWTClaimsSet().getExpirationTime();

        var verify= signedJWT.verify(verifier);

       if(!(verify && expityTime.after(new Date())))
           throw new AppException(ErrorCode.UNAUTHENTICATED);

       if(invalidtedTokenRepository.existsById(signedJWT.getJWTClaimsSet().getJWTID()))
           throw new AppException(ErrorCode.UNAUTHENTICATED);

        return signedJWT;

    }

    private String generateToken(User user){
       JWSHeader header = new JWSHeader(JWSAlgorithm.HS512);

        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject(String.valueOf(user.getId())) // Dùng user ID (duy nhất) thay vì fullName
                .issuer("devteira.com")
                .issueTime(new Date())
                .expirationTime(new Date(
                        Instant.now().plus(VALIDATION_DURATION, ChronoUnit.SECONDS).toEpochMilli()
                ))
                .jwtID(UUID.randomUUID().toString())
                .claim("scope",buildScope(user))
                .claim("userId", user.getId())
                .claim("fullName", user.getFullName()) // Thêm fullName vào claim để frontend có thể hiển thị
                .build();

        Payload payload = new Payload(claimsSet.toJSONObject());

        JWSObject jwsObject=new JWSObject(header,payload);

        try {
            jwsObject.sign(new MACSigner(SIGNER_KEY.getBytes()));
            return  jwsObject.serialize();
        } catch (JOSEException e) {
            log.error("Cannot create token", e);
            throw new RuntimeException(e);

        }
    }

    private String buildScope(User user){
        StringJoiner stringJoiner=new StringJoiner(" ");
        if(!CollectionUtils.isEmpty(user.getRoles()))
            user.getRoles().forEach(role->{
                stringJoiner.add("ROLE_"+role.getNameRoles());
                if(CollectionUtils.isEmpty(role.getPermissions())){}
                  role.getPermissions().forEach(permission->stringJoiner.add(permission.getNamePermission()));
        });

        return stringJoiner.toString();
    }

}
