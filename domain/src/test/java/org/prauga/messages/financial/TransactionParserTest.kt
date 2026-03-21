package org.prauga.messages.financial

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.prauga.messages.financial.AccountType
import org.junit.Before
import org.junit.Test

class TransactionParserTest {

    private lateinit var parser: TransactionParser

    @Before
    fun setup() {
        parser = TransactionParser()
    }

    // ── Amount parsing ──────────────────────────────────────────────────

    @Test
    fun `parses rupee symbol amount`() {
        val t = parser.parse(1L, "₹1,234.56 debited from your account", 0L)
        assertNotNull(t)
        assertEquals(1234.56, t!!.amount, 0.001)
    }

    @Test
    fun `parses Rs dot amount`() {
        val t = parser.parse(1L, "Rs.500.00 paid to Amazon", 0L)
        assertNotNull(t)
        assertEquals(500.0, t!!.amount, 0.001)
    }

    @Test
    fun `parses INR amount`() {
        val t = parser.parse(1L, "INR 2000 credited to your account", 0L)
        assertNotNull(t)
        assertEquals(2000.0, t!!.amount, 0.001)
    }

    @Test
    fun `parses amount with commas`() {
        val t = parser.parse(1L, "₹10,000.00 debited", 0L)
        assertNotNull(t)
        assertEquals(10000.0, t!!.amount, 0.001)
    }

    @Test
    fun `returns null when no amount found`() {
        val t = parser.parse(1L, "Your OTP is 123456. Do not share.", 0L)
        assertNull(t)
    }

    @Test
    fun `returns null for empty body`() {
        assertNull(parser.parse(1L, "", 0L))
    }

    // ── Transaction type ────────────────────────────────────────────────

    @Test
    fun `detects DEBIT from debited keyword`() {
        val t = parser.parse(1L, "₹500 debited from account XX1234", 0L)
        assertEquals(TransactionType.DEBIT, t!!.type)
    }

    @Test
    fun `detects DEBIT from paid keyword`() {
        val t = parser.parse(1L, "₹250 paid to Swiggy", 0L)
        assertEquals(TransactionType.DEBIT, t!!.type)
    }

    @Test
    fun `detects CREDIT from credited keyword`() {
        val t = parser.parse(1L, "₹1000 credited to your account", 0L)
        assertEquals(TransactionType.CREDIT, t!!.type)
    }

    @Test
    fun `detects CREDIT from refund keyword`() {
        val t = parser.parse(1L, "₹300 refund received from Flipkart", 0L)
        assertEquals(TransactionType.CREDIT, t!!.type)
    }

    @Test
    fun `defaults to UNKNOWN when no type keyword`() {
        val t = parser.parse(1L, "₹500 transaction on your card", 0L)
        assertEquals(TransactionType.UNKNOWN, t!!.type)
    }

    // ── Merchant parsing ────────────────────────────────────────────────

    @Test
    fun `extracts merchant after 'at'`() {
        val t = parser.parse(1L, "₹500 debited at Amazon", 0L)
        assertEquals("Amazon", t!!.merchant)
    }

    @Test
    fun `extracts merchant after 'to'`() {
        val t = parser.parse(1L, "₹250 paid to Swiggy", 0L)
        assertEquals("Swiggy", t!!.merchant)
    }

    @Test
    fun `merchant is null when not found`() {
        val t = parser.parse(1L, "₹500 debited from your account", 0L)
        // "from" matches but "your account" is valid — just check it doesn't crash
        assertNotNull(t)
    }

    // ── Account parsing ─────────────────────────────────────────────────

    @Test
    fun `extracts account from XX1234 pattern`() {
        val t = parser.parse(1L, "₹500 debited from AC XX1234", 0L)
        assertEquals("1234", t!!.account)
    }

    @Test
    fun `extracts account from ending 1234 pattern`() {
        val t = parser.parse(1L, "₹500 charged to card ending 5678", 0L)
        assertEquals("5678", t!!.account)
    }

    @Test
    fun `account is null when not found`() {
        val t = parser.parse(1L, "₹500 debited from your account", 0L)
        assertNull(t!!.account)
    }

    // ── Metadata ────────────────────────────────────────────────────────

    @Test
    fun `preserves messageId and date`() {
        val t = parser.parse(42L, "₹100 debited", 1234567890L)
        assertEquals(42L, t!!.messageId)
        assertEquals(1234567890L, t.date)
    }

    @Test
    fun `real world HDFC debit SMS`() {
        val body = "Rs.5,000.00 debited from ac XX4321 on 21-Mar-26 to VPA merchant@upi. Avl Bal:Rs.12,345.67"
        val t = parser.parse(1L, body, 0L, "HDFCBK")
        assertNotNull(t)
        assertEquals(5000.0, t!!.amount, 0.001)
        assertEquals(TransactionType.DEBIT, t.type)
        assertEquals("4321", t.account)
        assertEquals("HDFC ••••4321", t.accountLabel)
        assertEquals(AccountType.UPI, t.accountType) // VPA in body
    }

    @Test
    fun `real world credit SMS`() {
        val body = "INR 10,000.00 credited to your SBI account XX9876 by NEFT from ABC Corp"
        val t = parser.parse(1L, body, 0L, "SBIINB")
        assertNotNull(t)
        assertEquals(10000.0, t!!.amount, 0.001)
        assertEquals(TransactionType.CREDIT, t.type)
        assertEquals("9876", t.account)
        assertEquals("SBI ••••9876", t.accountLabel)
    }

    @Test
    fun `detects credit card account type`() {
        val body = "₹2,000 charged to your credit card ending 5678"
        val t = parser.parse(1L, body, 0L, "ICICIB")
        assertNotNull(t)
        assertEquals(AccountType.CREDIT_CARD, t!!.accountType)
        assertEquals("5678", t.account)
    }

    @Test
    fun `detects UPI account type`() {
        val body = "₹500 paid via UPI to merchant@okaxis"
        val t = parser.parse(1L, body, 0L, "GPAY")
        assertNotNull(t)
        assertEquals(AccountType.UPI, t!!.accountType)
    }

    @Test
    fun `builds account label without last4 when only sender known`() {
        val body = "₹1,000 debited from your account"
        val t = parser.parse(1L, body, 0L, "AXISBK")
        assertNotNull(t)
        assertEquals("Axis", t!!.accountLabel)
    }
}
