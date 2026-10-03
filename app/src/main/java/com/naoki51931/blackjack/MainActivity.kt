package com.naoki51931.blackjack

import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

enum class Suit(val symbol:String){ SPADE("♠"), HEART("♥"), DIAMOND("♦"), CLUB("♣") }
data class Card(val rank:String,val suit:Suit)
fun deck()=Suit.entries.flatMap{s->listOf("A","2","3","4","5","6","7","8","9","10","J","Q","K").map{Card(it,s)}}.shuffled()
fun score(h:List<Card>):Int{var n=h.sumOf{if(it.rank=="A")11 else it.rank.toIntOrNull()?:10};var a=h.count{it.rank=="A"};while(n>21&&a-->0)n-=10;return n}
fun blackjack(h:List<Card>)=h.size==2&&score(h)==21

class MainActivity:ComponentActivity(){override fun onCreate(b:Bundle?){super.onCreate(b);setContent{BlackjackApp()}}}

@Composable fun BlackjackApp(){
 var showTitle by remember{mutableStateOf(true)}
 MaterialTheme(colorScheme=darkColorScheme(primary=Color(0xFFFFD166),background=Color(0xFF061A12),surface=Color(0xFF0C3B2B))){
  AnimatedContent(targetState=showTitle,label="screen"){title->if(title)TitleScreen{showTitle=false}else GameScreen{showTitle=true}}
 }
}

@Composable fun TitleScreen(onStart:()->Unit){
 Box(Modifier.fillMaxSize().background(Color(0xFF061A12)).statusBarsPadding().navigationBarsPadding().padding(24.dp),contentAlignment=Alignment.Center){
  Column(horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center){
   Text("♠   ♥",fontSize=54.sp,fontWeight=FontWeight.Black,color=Color.White)
   Spacer(Modifier.height(10.dp))
   Text("21",fontSize=104.sp,fontWeight=FontWeight.Black,color=Color(0xFFFFD166))
   Text("BLACK JACK",fontSize=35.sp,fontWeight=FontWeight.Black,color=Color.White,letterSpacing=2.sp)
   Spacer(Modifier.height(12.dp))
   Text("♣  A   K  ♦",fontSize=30.sp,fontWeight=FontWeight.Bold,color=Color(0xFFFFD166))
   Spacer(Modifier.height(44.dp))
   Button(onClick=onStart,Modifier.fillMaxWidth().height(58.dp)){Text("START",fontSize=22.sp,fontWeight=FontWeight.Black)}
   Spacer(Modifier.height(14.dp))
   Text("BLACKJACK 21",fontSize=13.sp,color=Color.LightGray,textAlign=TextAlign.Center)
  }
 }
}

@Composable fun GameScreen(onTitle:()->Unit){
 val context=LocalContext.current
 var shoe by remember{mutableStateOf(deck())};var hand by remember{mutableStateOf(emptyList<Card>())};var splitHand by remember{mutableStateOf<List<Card>?>(null)};var dealer by remember{mutableStateOf(emptyList<Card>())}
 var chips by remember{mutableIntStateOf(1000)};var bet by remember{mutableIntStateOf(50)};var activeBet by remember{mutableIntStateOf(0)};var wins by remember{mutableIntStateOf(0)};var streak by remember{mutableIntStateOf(0)}
 var reveal by remember{mutableStateOf(false)};var playing by remember{mutableStateOf(false)};var resolving by remember{mutableStateOf(false)};var msg by remember{mutableStateOf("PLACE YOUR BET")};var flash by remember{mutableStateOf(false)}
 fun buzz(ms:Long=45){(context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator)?.vibrate(VibrationEffect.createOneShot(ms,120))}
 fun payout(mult:Double){chips+=(activeBet*mult).toInt()}
 fun start(){if(bet>chips)return;val d=deck();chips-=bet;activeBet=bet;shoe=d.drop(4);hand=listOf(d[0],d[2]);dealer=listOf(d[1],d[3]);splitHand=null;reveal=false;playing=true;resolving=false;msg="MAKE 21";buzz();if(blackjack(hand)){msg="BLACKJACK!";playing=false;resolving=true;flash=true}}
 fun hit(){if(!playing||shoe.isEmpty())return;hand=hand+shoe.first();shoe=shoe.drop(1);buzz();if(score(hand)>21){msg="BUST!";playing=false;reveal=true;streak=0}else if(score(hand)==21){msg="21!";flash=true}}
 fun doubleDown(){if(playing&&hand.size==2&&chips>=activeBet){chips-=activeBet;activeBet*=2;hit();if(score(hand)<=21){playing=false;resolving=true}}}
 fun split(){if(playing&&hand.size==2&&hand[0].rank==hand[1].rank&&chips>=activeBet&&shoe.size>=2){chips-=activeBet;val a=hand[0];val b=hand[1];hand=listOf(a,shoe[0]);splitHand=listOf(b,shoe[1]);shoe=shoe.drop(2);msg="SPLIT • PLAY LEFT";buzz(80)}}
 suspend fun dealerRun(){reveal=true;while(score(dealer)<17&&shoe.isNotEmpty()){delay(480);dealer=dealer+shoe.first();shoe=shoe.drop(1)};val p=score(hand);val d=score(dealer);when{p>21->{msg="BUST!";streak=0};d>21||p>d->{payout(2.0);wins++;streak++;msg="YOU WIN!";flash=true};p==d->{payout(1.0);msg="PUSH"};else->{msg="DEALER WINS";streak=0}};resolving=false;playing=false;buzz(100)}
 LaunchedEffect(resolving){if(resolving){delay(if(msg=="BLACKJACK!")900 else 200);if(msg=="BLACKJACK!"){payout(2.5);wins++;streak++;resolving=false;reveal=true}else dealerRun()}}
 LaunchedEffect(flash){if(flash){delay(650);flash=false}}
 val pulse by animateFloatAsState(if(flash)1.18f else 1f,spring(dampingRatio=.45f),label="pulse")
 val shake by animateFloatAsState(if(msg=="BUST!")3f else 0f,spring(dampingRatio=.35f),label="shake")
 Box(Modifier.fillMaxSize().background(if(flash)Color(0xFF4A3B0B) else Color(0xFF061A12)).rotate(shake)){
  Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal=14.dp,vertical=8.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.SpaceBetween){
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){TextButton(onClick=onTitle,contentPadding=PaddingValues(0.dp)){Text("‹ TITLE",color=Color.LightGray,fontSize=12.sp)};Text("♠ BLACKJACK",color=Color.White,fontWeight=FontWeight.Black,fontSize=20.sp);Column(horizontalAlignment=Alignment.End){Text("● $chips",color=Color(0xFFFFD166),fontWeight=FontWeight.Bold);Text("W $wins  🔥$streak",color=Color.White,fontSize=12.sp)}}
   Column(horizontalAlignment=Alignment.CenterHorizontally){Text("DEALER",color=Color.LightGray,fontSize=13.sp);CardRow(dealer,!reveal);Text(if(reveal)score(dealer).toString() else "?",fontSize=23.sp,color=Color.White,fontWeight=FontWeight.Bold)}
   Text(msg,Modifier.scale(pulse),fontSize=27.sp,fontWeight=FontWeight.Black,color=if(msg=="BUST!")Color(0xFFFF5252) else Color(0xFFFFD166))
   Column(horizontalAlignment=Alignment.CenterHorizontally){Text("YOU • ${score(hand)}",color=Color.White,fontSize=18.sp,fontWeight=FontWeight.Bold);CardRow(hand,false);splitHand?.let{Text("SPLIT • ${score(it)}",color=Color(0xFFFFD166));CardRow(it,false)}}
   if(!playing&&!resolving){Column(Modifier.fillMaxWidth(),horizontalAlignment=Alignment.CenterHorizontally){Text("BET $bet",color=Color.White,fontWeight=FontWeight.Bold);Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){listOf(25,50,100,250).forEach{OutlinedButton({bet=it},Modifier.weight(1f),contentPadding=PaddingValues(horizontal=2.dp),enabled=it<=chips){Text("$it")}}};Spacer(Modifier.height(4.dp));Button({start()},Modifier.fillMaxWidth().height(48.dp)){Text("DEAL",fontSize=19.sp,fontWeight=FontWeight.Black)}}}
   else if(playing){Column(Modifier.fillMaxWidth()){Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){Button({hit()},Modifier.weight(1f)){Text("HIT")};Button({playing=false;resolving=true},Modifier.weight(1f)){Text("STAND")}};Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){OutlinedButton({doubleDown()},Modifier.weight(1f),enabled=hand.size==2&&chips>=activeBet){Text("DOUBLE")};OutlinedButton({split()},Modifier.weight(1f),enabled=hand.size==2&&hand[0].rank==hand[1].rank&&chips>=activeBet){Text("SPLIT")}}}}
   else CircularProgressIndicator(color=Color(0xFFFFD166))
  }
  AnimatedVisibility(flash,enter=fadeIn(),exit=fadeOut(),modifier=Modifier.align(Alignment.Center)){Box(Modifier.size(220.dp).border(4.dp,Color(0xFFFFD166),CircleShape),contentAlignment=Alignment.Center){Text(if(msg=="BLACKJACK!")"21" else "★",fontSize=80.sp,fontWeight=FontWeight.Black,color=Color(0xFFFFD166),modifier=Modifier.alpha(.7f))}}
 }
}

@Composable fun CardRow(cards:List<Card>,hidden:Boolean){Row(horizontalArrangement=Arrangement.spacedBy((-15).dp)){cards.forEachIndexed{i,c->PlayingCard(c,hidden&&i==1,i)}}}
@Composable fun PlayingCard(card:Card,hidden:Boolean,index:Int){var inView by remember(card){mutableStateOf(false)};LaunchedEffect(card){delay(index*70L);inView=true};val r by animateFloatAsState(if(inView)0f else -20f,tween(300),label="deal");val s by animateFloatAsState(if(inView)1f else .72f,tween(300),label="scale");Card(Modifier.size(70.dp,96.dp).rotate(r).scale(s),shape=RoundedCornerShape(9.dp)){if(hidden)Box(Modifier.fillMaxSize().background(Color(0xFF8B1830)).border(3.dp,Color.White,RoundedCornerShape(9.dp)),contentAlignment=Alignment.Center){Text("♠",fontSize=34.sp,color=Color.White)}else{val red=card.suit==Suit.HEART||card.suit==Suit.DIAMOND;val col=if(red)Color.Red else Color.Black;Column(Modifier.fillMaxSize().padding(6.dp),verticalArrangement=Arrangement.SpaceBetween){Text(card.rank,fontSize=17.sp,fontWeight=FontWeight.Black,color=col);Text(card.suit.symbol,fontSize=28.sp,color=col,modifier=Modifier.align(Alignment.CenterHorizontally));Text(card.rank,fontSize=17.sp,fontWeight=FontWeight.Black,color=col,modifier=Modifier.align(Alignment.End))}}}}
