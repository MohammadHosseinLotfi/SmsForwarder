package com.example.smsforwarder.data

enum class DeliveryState {
    QUEUED,          // ذخیره شده در صف
    WAITING_NETWORK, // منتظر اتصال اینترنت (فقط HTTP)
    IN_FLIGHT,       // در حال ارسال
    RETRY_WAIT,      // منتظر تلاش مجدد
    BLOCKED,         // خطای مجوز یا سیم‌کارت
    SUCCEEDED,       // ارسال موفق
    NEEDS_REVIEW,    // پایان تلاش‌های خودکار / خطای دائمی
    UNKNOWN,         // وضعیت نامشخص (قطع ارتباط در لحظه ارسال)
    CANCELLED        // لغو شده توسط کاربر
}