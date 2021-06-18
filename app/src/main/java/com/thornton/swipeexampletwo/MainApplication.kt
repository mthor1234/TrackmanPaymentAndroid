package com.thornton.swipeexampletwo

import android.app.Application
import com.stripe.android.PaymentConfiguration

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        PaymentConfiguration.init(
            applicationContext,
            "pk_test_51ItP0RHGBf1FRE091l20RehxW2A5QkWE02MXUIzq3cyNzkOnCT7GDVM3zoilNxYkF1LcpoEPPYFiQh9cDfaHLjqN00T1jbIlE4"
        )
    }
}
