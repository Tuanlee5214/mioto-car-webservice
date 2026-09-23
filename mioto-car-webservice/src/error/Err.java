/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package error;

/**
 *
 * @author tuanlee
 */
public class Err {
     
    public static final int SUCCESS        =  0;
    public static final int FAIL           = -1;
    public static final int BAD_REQUEST    = -1003;   // caller sent something invalid
    public static final int NOT_FOUND      = -1004;   // query matched zero rows
    public static final int UNAUTHORIZED   = -1005;
    public static final int FORBIDDEN      = -1006;
    public static final int CONFLICT       = -1007;
    public static final int NO_CONNECTION  = -1001;   // could not borrow / could not reach
    public static final int BAD_CONNECTION = -1002;   // reached it, failed after all retries

    private Err() { }

    public static boolean isSuccess(long e) { return e >= 0; }
    public static boolean isFail(long e)    { return e <  0; }
    public static boolean isNotFound(long e) { return e == NOT_FOUND; }

    /**
     * A transport/connection failure, as opposed to "the row is not there".
     * The cache in Step 10 must be able to tell these apart.
     */
    public static boolean isNetworkError(long e) {
        return e == NO_CONNECTION || e == BAD_CONNECTION;
    }
}
