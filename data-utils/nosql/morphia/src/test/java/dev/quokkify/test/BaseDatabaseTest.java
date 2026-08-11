package dev.quokkify.test;

import dev.quokkify.listener.lifecycle.SuiteListener;

import org.testng.annotations.Listeners;

@Listeners({SuiteListener.class})
abstract class BaseDatabaseTest {
}
