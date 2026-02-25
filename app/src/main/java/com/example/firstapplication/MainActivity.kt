package com.example.firstapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import com.google.gson.annotations.SerializedName

// --- MODELS ---
data class Person(
    val name: String,
    val height: String,
    val mass: String,
    @SerializedName("birth_year") val birthYear: String
)

data class SwapiResponse(
    val results: List<Person>
)

// --- API SERVICE ---
interface SwapiService {
    @GET("people/")
    suspend fun getPeople(): SwapiResponse

    companion object {
        private var instance: SwapiService? = null
        fun getInstance(): SwapiService {
            if (instance == null) {
                instance = Retrofit.Builder()
                    .baseUrl("https://swapi.dev/api/")
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                    .create(SwapiService::class.java)
            }
            return instance!!
        }
    }
}

// --- VIEWMODEL ---
class PeopleViewModel : ViewModel() {
    var peopleState by mutableStateOf<List<Person>>(emptyList())
    var isLoading by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)

    init {
        fetchPeople()
    }

    private fun fetchPeople() {
        viewModelScope.launch {
            isLoading = true
            try {
                val response = SwapiService.getInstance().getPeople()
                peopleState = response.results
            } catch (e: Exception) {
                errorMessage = e.message
            } finally {
                isLoading = false
            }
        }
    }
}

// --- UI THEME ---
private val StarWarsYellow = Color(0xFFFFE81F)
private val StarWarsBlack = Color(0xFF121212)

@Composable
fun StarWarsTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = StarWarsYellow,
            background = StarWarsBlack,
            surface = Color(0xFF1E1E1E),
            onPrimary = Color.Black,
            onBackground = Color.White,
            onSurface = Color.White
        ),
        content = content
    )
}

// --- SCREENS ---
@Composable
fun PeopleListScreen(viewModel: PeopleViewModel, onPersonClick: (String) -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        if (viewModel.isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = StarWarsYellow)
        } else if (viewModel.errorMessage != null) {
            Text(text = "Erro: ${viewModel.errorMessage}", color = Color.Red, modifier = Modifier.align(Alignment.Center))
        } else {
            LazyColumn(modifier = Modifier.padding(16.dp)) {
                item {
                    Text(
                        "STAR WARS PEOPLE",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = StarWarsYellow,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }
                items(viewModel.peopleState) { person ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .clickable { onPersonClick(person.name) },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Text(
                            text = person.name,
                            modifier = Modifier.padding(16.dp),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PersonDetailScreen(person: Person, onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(StarWarsBlack)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(text = person.name, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = StarWarsYellow)
        Divider(color = StarWarsYellow, thickness = 2.dp)
        
        DetailItem("Height", "${person.height} cm")
        DetailItem("Mass", "${person.mass} kg")
        DetailItem("Birth Year", person.birthYear)

        Spacer(modifier = Modifier.weight(1f))
        
        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = StarWarsYellow)
        ) {
            Text("Voltar", color = Color.Black)
        }
    }
}

@Composable
fun DetailItem(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = label, color = Color.Gray, fontSize = 16.sp)
        Text(text = value, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

// --- MAIN ACTIVITY ---
class MainActivity : ComponentActivity() {
    private val viewModel: PeopleViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            StarWarsTheme {
                val navController = rememberNavController()
                NavHost(navController = navController, startDestination = "list") {
                    composable("list") {
                        PeopleListScreen(viewModel) { personName ->
                            navController.navigate("detail/$personName")
                        }
                    }
                    composable(
                        "detail/{name}",
                        arguments = listOf(navArgument("name") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val name = backStackEntry.arguments?.getString("name")
                        val person = viewModel.peopleState.find { it.name == name }
                        if (person != null) {
                            PersonDetailScreen(person) { navController.popBackStack() }
                        }
                    }
                }
            }
        }
    }
}