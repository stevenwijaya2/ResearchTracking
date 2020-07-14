package umn.ac.id.researchtracking;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class Login extends AppCompatActivity {
    //declare variable
    private TextView text_Register;
    private EditText et_Email,et_Password;
    FirebaseAuth mAuth = FirebaseAuth.getInstance();
    Button btn_login;
    Boolean valid = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        //assign variable value by view
        text_Register = findViewById(R.id.text_register);
        et_Email = findViewById(R.id.et_email);
        et_Password = findViewById(R.id.et_password);
        btn_login = findViewById(R.id.btn_login);

        text_Register.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent gotoregister = new Intent(getApplicationContext(), Register.class);
                startActivityForResult(gotoregister, 1);
            }
        });

        btn_login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                valid = true;
                Log.d("Register","email : "+et_Email.getText().toString());
                Log.d("Register","pass : "+et_Password.getText().toString());

                if(et_Email.getText().toString().isEmpty()){
                    et_Email.setError("Please Fill this form");
                    valid = false;
                }
                if(et_Password.getText().toString().isEmpty()){
                    et_Password.setError("Please Fill this form");
                    valid = false;
                }
                if(valid){
                    LoginUser(et_Email.getText().toString(),et_Password.getText().toString());

                }
            }
        });

    }
    private void LoginUser(String email,String password){
        Log.i("email",email);
        Log.i("password",password);
        Log.i("mAuth", String.valueOf(mAuth));
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            if(mAuth.getCurrentUser().isEmailVerified()){
                                //verified email
                                Log.d("Login User", "signInWithEmail:success");
                                Intent gotoMain = new Intent(getApplicationContext(), MainActivity.class);
                                startActivityForResult(gotoMain, 1);
                            }
                            else{
                                Toast.makeText(Login.this, "Verification Account Failed ! Please Check Your Email Account", Toast.LENGTH_SHORT).show();
                            }

                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w("Login User", "signInWithEmail:failure", task.getException());
                            Toast.makeText(Login.this, "Wrong Email or Password !", Toast.LENGTH_SHORT).show();
                        }

                        // ...
                    }
                });
    }

}
