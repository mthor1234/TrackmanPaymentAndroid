package com.thornton.swipeexampletwo

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.stripe.android.PaymentConfiguration
import com.stripe.android.paymentsheet.PaymentOptionCallback
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResult
import com.stripe.android.paymentsheet.PaymentSheetResultCallback
import com.stripe.android.paymentsheet.model.PaymentOption
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class CheckoutActivity : AppCompatActivity() {
    private lateinit var flowController: PaymentSheet.FlowController
    private lateinit var paymentMethodButton: Button
    private lateinit var payButton: Button
    private lateinit var frameLayout: FrameLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // instantiate view and buttons
        setContentView(R.layout.activity_checkout)
        payButton = findViewById(R.id.payButton)
        paymentMethodButton = findViewById(R.id.paymentMethodButton)
        frameLayout = findViewById(R.id.frameLayout)

        paymentMethodButton.setOnClickListener {
            toggleProgressBar()
        }

        PaymentConfiguration.init(this, STRIPE_PUBLISHABLE_KEY)
        payButton.isEnabled = false

        val paymentOptionCallback = PaymentOptionCallback { paymentOption ->
            onPaymentOption(paymentOption)
        }

        val paymentSheetResultCallback = PaymentSheetResultCallback { paymentSheetResult ->
            onPaymentSheetResult(paymentSheetResult)
        }

        flowController = PaymentSheet.FlowController.create(
            this,
            paymentOptionCallback,
            paymentSheetResultCallback
        )

        fetchInitData()
    }



    private fun fetchInitData() {
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val json = "{}"
        val body = json.toRequestBody(mediaType)
        val request = Request.Builder()
            .url(BACKEND_URL + "payment-sheet")
            .post(body)
            .build()

        OkHttpClient().newCall(request)
            .enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    // Handle failure
                }

                override fun onResponse(call: Call, response: Response) {
                    if (!response.isSuccessful) {
                        // Handle failure
                    } else {
                        val responseData = response.body?.string()
                        val responseJson =
                            responseData?.let { JSONObject(it) } ?: JSONObject()

                        val customerId = responseJson.getString("customer")
                        val ephemeralKeySecret = responseJson.getString("ephemeralKey")
                        val paymentIntentClientSecret = responseJson.getString("paymentIntent")

                        configureFlowController(
                            paymentIntentClientSecret,
                            customerId,
                            ephemeralKeySecret
                        )
                    }
                }
            }
            )
    }

    private fun configureFlowController(
        paymentIntentClientSecret: String,
        customerId: String,
        ephemeralKeySecret: String
    ) {
        // TODO: 6/17/2021 Testing
//        flowController.configure(
        flowController.configureWithPaymentIntent(
            paymentIntentClientSecret = paymentIntentClientSecret,
            configuration = PaymentSheet.Configuration(
                merchantDisplayName = "Example, Inc.",
                customer = PaymentSheet.CustomerConfiguration(
                    id = customerId,
                    ephemeralKeySecret = ephemeralKeySecret
                )
            )
        ) { isReady, error ->
            if (isReady) {
                onFlowControllerReady()
            } else {
                // handle FlowController configuration failure
            }
        }
    }

    private fun onFlowControllerReady() {
        paymentMethodButton.setOnClickListener {
            flowController.presentPaymentOptions()
        }
        payButton.setOnClickListener {
            onCheckout()
        }
        paymentMethodButton.isEnabled = true
        onPaymentOption(flowController.getPaymentOption())
    }

    private fun onPaymentOption(paymentOption: PaymentOption?) {
        if (paymentOption != null) {
            paymentMethodButton.text = paymentOption.label
            paymentMethodButton.setCompoundDrawablesRelativeWithIntrinsicBounds(
                paymentOption.drawableResourceId,
                0,
                0,
                0
            )
            payButton.isEnabled = true
        } else {
            paymentMethodButton.text = "Select"
            paymentMethodButton.setCompoundDrawablesRelativeWithIntrinsicBounds(
                null,
                null,
                null,
                null
            )
            payButton.isEnabled = false
        }
    }

    private fun onCheckout() {
        flowController.confirm()
    }

    private fun toggleProgressBar(){
        println("Toggle Progress Bar")
        if(frameLayout.visibility == View.VISIBLE){
            frameLayout.visibility = View.GONE
        }else{
            frameLayout.visibility = View.VISIBLE
        }
    }

    private fun onPaymentSheetResult(
        paymentSheetResult: PaymentSheetResult
    ) {
        when (paymentSheetResult) {
            is PaymentSheetResult.Canceled -> {
                Toast.makeText(
                    this,
                    "Payment Canceled",
                    Toast.LENGTH_LONG
                ).show()
            }
            is PaymentSheetResult.Failed -> {
                Toast.makeText(
                    this,
                    "Payment Failed. See logcat for details",
                    Toast.LENGTH_LONG
                ).show()
                Log.e("App", "Got error: ${paymentSheetResult.error}")
            }
            is PaymentSheetResult.Completed -> {
                Toast.makeText(
                    this,
                    "Payment Complete",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }


    private companion object {
        private const val BACKEND_URL = "https://example.com/"
        private const val STRIPE_PUBLISHABLE_KEY = "pk_test_..."
    }


}