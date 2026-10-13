package com.example

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.CharacterEntity
import com.example.model.ReputationRank
import com.example.viewmodel.GameViewModel
import com.example.viewmodel.ShowdownAction
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Криминальная Россия", appName)
  }

  @Test
  fun `test reputation rank transitions`() {
    assertEquals(ReputationRank.SHNYR, ReputationRank.fromRespect(0))
    assertEquals(ReputationRank.PATSAN, ReputationRank.fromRespect(50))
    assertEquals(ReputationRank.BYK, ReputationRank.fromRespect(150))
    assertEquals(ReputationRank.BRIGADIR, ReputationRank.fromRespect(350))
    assertEquals(ReputationRank.SMOTRYASHIY, ReputationRank.fromRespect(750))
    assertEquals(ReputationRank.AVTORITET, ReputationRank.fromRespect(1300))
    assertEquals(ReputationRank.VOR_V_ZAKONE, ReputationRank.fromRespect(2000))
  }

  @Test
  fun `test room character entity persistence`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
    val dao = db.characterDao()

    val hero = CharacterEntity(
      id = 1,
      nickname = "Седой",
      strength = 45,
      respect = 120,
      money = 50000L,
      health = 90
    )

    dao.insertOrUpdate(hero)

    val savedHero = dao.getCharacterSync()
    assertNotNull(savedHero)
    assertEquals("Седой", savedHero?.nickname)
    assertEquals(45, savedHero?.strength)
    assertEquals(120, savedHero?.respect)
    assertEquals(50000L, savedHero?.money)
    assertEquals(90, savedHero?.health)

    // Update core stats
    dao.updateCoreStats(strength = 50, respect = 150, money = 75000L, health = 100)
    val updatedHero = dao.getCharacterSync()
    assertNotNull(updatedHero)
    assertEquals(50, updatedHero?.strength)
    assertEquals(150, updatedHero?.respect)
    assertEquals(75000L, updatedHero?.money)
    assertEquals(100, updatedHero?.health)

    db.close()
  }

  @Test
  fun `test showdown combat action and reactivity`() {
    val application = ApplicationProvider.getApplicationContext<Application>()
    val vm = GameViewModel(application)

    vm.startShowdown(
      rivalName = "Тестовый враг",
      rivalTitle = "Бык",
      maxHp = 50,
      attack = 5,
      rewardMoney = 10000L,
      rewardRespect = 20
    )

    val initialShowdown = vm.uiState.value.activeShowdown
    assertNotNull(initialShowdown)
    assertEquals(50, initialShowdown?.rivalCurrentHp)
    assertEquals(1, initialShowdown?.battleLog?.size)

    // Perform Punch action
    vm.performShowdownAction(ShowdownAction.PUNCH)

    val updatedShowdown = vm.uiState.value.activeShowdown
    assertNotNull(updatedShowdown)
    assertTrue((updatedShowdown?.rivalCurrentHp ?: 50) < 50)
    assertTrue((updatedShowdown?.battleLog?.size ?: 0) > 1)
  }
}
