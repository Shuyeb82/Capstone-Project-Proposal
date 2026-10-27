package com.labbooking.dao;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// পাসওয়ার্ড হ্যাশিংই একমাত্র নতুন অ্যালগরিদম যেটা DB ছাড়াই পরীক্ষা করা যায়,
// আর এটা ভাঙলে লগইন পুরো অচল — তাই Phase 3 এই একটা টেস্ট রেখে যায়।
class UserDaoPasswordTest {

    // seed.sql এ বসানো অ্যাডমিনের আসল হ্যাশ। কোড আর seed একই ফরম্যাট মানছে কি না,
    // সেটা এই একটা assert ই ধরে ফেলবে।
    private static final String ADMIN_HASH =
            "a1b2c3d4e5f60718$b90f4bebd6a802058aa94ae1342fa683358e42caa59b78f1c1fbbe854ff18c07";

    @Test
    void seedHashAcceptsDemoPassword() {
        assertTrue(UserDao.verify("password123", ADMIN_HASH));
        assertFalse(UserDao.verify("Password123", ADMIN_HASH));
        assertFalse(UserDao.verify("", ADMIN_HASH));
    }

    @Test
    void newHashRoundTrips() {
        String hash = UserDao.hash("correct horse battery");
        assertTrue(UserDao.verify("correct horse battery", hash));
        assertFalse(UserDao.verify("wrong horse battery", hash));
        // salt আলাদা হওয়ায় একই পাসওয়ার্ডের দুইটা হ্যাশ কখনো এক হবে না।
        assertNotEquals(hash, UserDao.hash("correct horse battery"));
    }

    @Test
    void brokenHashIsRejectedNotCrashed() {
        assertFalse(UserDao.verify("password123", "no-dollar-sign-here"));
        assertFalse(UserDao.verify(null, ADMIN_HASH));
    }
}
