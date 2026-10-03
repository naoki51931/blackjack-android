package com.naoki51931.blackjack

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.random.Random

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContent { BlackjackApp() } }
}

enum class Suit(val symbol:String){ SPADE("♠"), HEART("♥"), DIAMOND("♦"), CLUB("♣") }
data class Card(val rank:String,val suit:Suit)
fun deck()=Suit.entries.flatMap { s -> listOf("A","2","3","4","5","6","7","8","9","10","J","Q","K").map{Card(it,s)} }.shuffled()
fun score(cards:List<Card>):Int { var total=cards.sumOf { if(it.rank=="A") 11 else it.rank.toIntOrNull()?:10 }; var aces=cards.count{it.rank=="A"}; while(total>21&&aces-->0) total-=10; return total }

@Composable fun BlackjackApp(){
    var cards by remember { mutableStateOf(deck()) }; var player by remember { mutableStateOf(emptyList<Card>()) }; var dealer by remember { mutableStateOf(emptyList<Card>()) }
    var over by remember { mutableStateOf(false) }; var reveal by remember { mutableStateOf(false) }; var message by remember { mutableStateOf("BLACKJACK 21") }; var wins by remember { mutableIntStateOf(0) }
    fun drawPlayer(){ if(cards.isNotEmpty()){ player=player+cards.first(); cards=cards.drop(1); if(score(player)>21){message="BUST!";over=true;reveal=true} } }
    fun newRound(){ val d=deck(); cards=d.drop(4); player=listOf(d[0],d[2]); dealer=listOf(d[1],d[3]); over=false; reveal=false; message="MAKE 21" }
    LaunchedEffect(Unit){ newRound() }
    suspend fun dealerPlay(){ reveal=true; while(score(dealer)<17){ delay(550); dealer=dealer+cards.first(); cards=cards.drop(1) }; val p=score(player);val d=score(dealer); message=when{d>21||p>d->{wins++;"YOU WIN!"};p==d->"PUSH";else->"DEALER WINS"};over=true }
    val shake by animateFloatAsState(if(message=="BUST!") 2f else 0f, spring(dampingRatio=Spring.DampingRatioHighBouncy),label="shake")
    MaterialTheme(colorScheme=darkColorScheme(primary=Color(0xFFFFD166),background=Color(0xFF071A13),surface=Color(0xFF0B3D2E))){
        Box(Modifier.fillMaxSize().background(Color(0xFF071A13)).rotate(shake)){
            Column(Modifier.fillMaxSize().padding(18.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.SpaceBetween){
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){ Text("♠ 21",fontSize=25.sp,fontWeight=FontWeight.Black,color=Color.White);Text("WINS  $wins",color=Color(0xFFFFD166),fontWeight=FontWeight.Bold) }
                Column(horizontalAlignment=Alignment.CenterHorizontally){Text("DEALER",color=Color.LightGray);Spacer(Modifier.height(8.dp));CardRow(dealer,hideSecond=!reveal);Text(if(reveal) score(dealer).toString() else "?",fontSize=30.sp,color=Color.White,fontWeight=FontWeight.Bold)}
                Text(message,fontSize=34.sp,fontWeight=FontWeight.Black,color=if(message=="BUST!")Color(0xFFFF5252) else Color(0xFFFFD166))
                Column(horizontalAlignment=Alignment.CenterHorizontally){Text("YOU  •  ${score(player)}",fontSize=22.sp,color=Color.White,fontWeight=FontWeight.Bold);Spacer(Modifier.height(8.dp));CardRow(player,false)}
                if(!over) Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(12.dp)){Button({drawPlayer()},Modifier.weight(1f)){Text("HIT",fontSize=20.sp)};Button(onClick={ over=true },Modifier.weight(1f)){Text("STAND",fontSize=20.sp)}} else Button({newRound()},Modifier.fillMaxWidth()){Text("NEW ROUND",fontSize=20.sp)}
                if(over && message=="DEALER WINS"){} 
            }
            if(!over) LaunchedEffect(over){ }
        }
    }
    if(over && !reveal && message!="BUST!") LaunchedEffect(over){ dealerPlay() }
}

@Composable fun CardRow(cards:List<Card>,hideSecond:Boolean){ Row(horizontalArrangement=Arrangement.spacedBy((-18).dp)){ cards.forEachIndexed{i,c-> PlayingCard(c,hideSecond&&i==1,i) } } }
@Composable fun PlayingCard(card:Card,hidden:Boolean,index:Int){
    var entered by remember(card){ mutableStateOf(false) }; LaunchedEffect(card){ delay(index*80L);entered=true }
    val angle by animateFloatAsState(if(entered)0f else -12f,tween(320),label="deal")
    Card(Modifier.size(82.dp,116.dp).rotate(angle),shape=RoundedCornerShape(10.dp)){
        if(hidden) Box(Modifier.fillMaxSize().background(Color(0xFF9B1C31)).border(4.dp,Color.White,RoundedCornerShape(10.dp)),contentAlignment=Alignment.Center){Text("♠",fontSize=42.sp,color=Color.White)}
        else { val red=card.suit==Suit.HEART||card.suit==Suit.DIAMOND; Column(Modifier.fillMaxSize().padding(8.dp),verticalArrangement=Arrangement.SpaceBetween){Text(card.rank,fontSize=21.sp,fontWeight=FontWeight.Black,color=if(red)Color.Red else Color.Black);Text(card.suit.symbol,fontSize=35.sp,color=if(red)Color.Red else Color.Black,modifier=Modifier.align(Alignment.CenterHorizontally));Text(card.rank,fontSize=21.sp,fontWeight=FontWeight.Black,color=if(red)Color.Red else Color.Black,modifier=Modifier.align(Alignment.End))} }
    }
}
