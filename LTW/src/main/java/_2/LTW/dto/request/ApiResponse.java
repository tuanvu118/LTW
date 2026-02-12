package _2.LTW.dto.request;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    @Builder.Default
    int code = 1000;

    String message;
    T result;

    public static <T> ApiResponse<T> ok(T data){

        return ApiResponse.<T>builder()
                .message("Success")
                .result(data)
                .build();

    }

    public static <T> ApiResponse<T> ok(T data, String message){

        return ApiResponse.<T>builder()
                .message(message)
                .result(data)
                .build();

    }

    public static <T> ApiResponse<T> error(int code, String message){

        return ApiResponse.<T>builder()
                .code(code)
                .message(message)
                .build();

    }

}
