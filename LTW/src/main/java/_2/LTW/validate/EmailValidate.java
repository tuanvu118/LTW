package _2.LTW.validate;

public class EmailValidate {
    
    
    public static boolean isValid(String email) {
        return email.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
    }
}
