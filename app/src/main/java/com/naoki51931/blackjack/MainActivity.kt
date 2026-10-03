package com.naoki51931.blackjack

import android.content.Context
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

enum class Suit(val symbol:String){SPADE("♠"),HEART("♥"),DIAMOND("♦"),CLUB("♣")}
data class Card(val rank:String,val suit:Suit)
fun deck()=Suit.entries.flatMap{s->listOf("A","2","3","4","5","6","7","8","9","10","J","Q","K").map{Card(it,s)}}.shuffled()
fun score(h:List<Card>):Int{var n=h.sumOf{if(it.rank=="A")11 else it.rank.toIntOrNull()?:10};var a=h.count{it.rank=="A"};while(n>21&&a-->0)n-=10;return n}
fun blackjack(h:List<Card>)=h.size==2&&score(h)==21
fun cardsText(h:List<Card>)=h.joinToString(" "){"${it.rank}${it.suit.symbol}"}

class MainActivity:ComponentActivity(){override fun onCreate(b:Bundle?){super.onCreate(b);setContent{BlackjackApp()}}}

@Composable fun BlackjackApp(){
 val context=LocalContext.current;val prefs=remember{context.getSharedPreferences("blackjack",Context.MODE_PRIVATE)}
 var apiKey by remember{mutableStateOf(prefs.getString("openrouter_key","")?:"")};var difficulty by remember{mutableStateOf(prefs.getString("difficulty","弱い")?:"弱い")};var showTitle by remember{mutableStateOf(true)}
 MaterialTheme(colorScheme=darkColorScheme(primary=Color(0xFFFFD166),background=Color(0xFF061A12),surface=Color(0xFF0C3B2B))){AnimatedContent(targetState=showTitle,label="screen"){title->if(title)TitleScreen(apiKey,{apiKey=it},difficulty,{difficulty=it},{prefs.edit().putString("openrouter_key",apiKey).putString("difficulty",difficulty).apply();showTitle=false})else GameScreen(apiKey,difficulty){showTitle=true}}}
}

@Composable fun TitleScreen(apiKey:String,onKey:(String)->Unit,difficulty:String,onDifficulty:(String)->Unit,onStart:()->Unit){
 Box(Modifier.fillMaxSize().background(Color(0xFF061A12)).statusBarsPadding().navigationBarsPadding().padding(24.dp),contentAlignment=Alignment.Center){Column(horizontalAlignment=Alignment.CenterHorizontally){Text("♠   ♥",fontSize=42.sp,fontWeight=FontWeight.Black,color=Color.White);Text("21",fontSize=82.sp,fontWeight=FontWeight.Black,color=Color(0xFFFFD166));Text("BLACK JACK",fontSize=30.sp,fontWeight=FontWeight.Black,color=Color.White);Text("AI DEALER • Gemini 3 Flash",fontSize=13.sp,color=Color(0xFFFFD166));Spacer(Modifier.height(22.dp));OutlinedTextField(apiKey,onKey,label={Text("OpenRouter API Key")},singleLine=true,visualTransformation=PasswordVisualTransformation(),keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Password),modifier=Modifier.fillMaxWidth());Spacer(Modifier.height(14.dp));Text("ディーラーの強さ",color=Color.White,fontWeight=FontWeight.Bold);Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)){if(difficulty=="弱い")Button({onDifficulty("弱い")},Modifier.weight(1f)){Text("弱い")}else OutlinedButton({onDifficulty("弱い")},Modifier.weight(1f)){Text("弱い")};if(difficulty=="強い")Button({onDifficulty("強い")},Modifier.weight(1f)){Text("強い")}else OutlinedButton({onDifficulty("強い")},Modifier.weight(1f)){Text("強い")}};Text(if(difficulty=="強い")"強い：カード状況と会話を慎重に分析し、ブラフも使います" else "弱い：読み違いや下手なブラフもあります",fontSize=11.sp,color=Color.LightGray);Spacer(Modifier.height(18.dp));Button(onStart,Modifier.fillMaxWidth().height(54.dp)){Text("START",fontSize=21.sp,fontWeight=FontWeight.Black)}}}
}

@Composable fun GameScreen(apiKey:String,difficulty:String,onTitle:()->Unit){
 val context=LocalContext.current;val scope=rememberCoroutineScope();var shoe by remember{mutableStateOf(deck())};var hand by remember{mutableStateOf(emptyList<Card>())};var dealer by remember{mutableStateOf(emptyList<Card>())};var chips by remember{mutableIntStateOf(1000)};var bet by remember{mutableIntStateOf(50)};var activeBet by remember{mutableIntStateOf(0)};var wins by remember{mutableIntStateOf(0)};var streak by remember{mutableIntStateOf(0)};var reveal by remember{mutableStateOf(false)};var playing by remember{mutableStateOf(false)};var resolving by remember{mutableStateOf(false)};var msg by remember{mutableStateOf("PLACE YOUR BET")};var flash by remember{mutableStateOf(false)};var dealerTalk by remember{mutableStateOf("いらっしゃいませ。勝負を始めましょう。")};var chat by remember{mutableStateOf("")};var aiLoading by remember{mutableStateOf(false)}
 fun buzz(ms:Long=45){(context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator)?.vibrate(VibrationEffect.createOneShot(ms,120))};fun payout(mult:Double){chips+=(activeBet*mult).toInt()}
 fun situation():String="難易度=$difficulty。プレイヤーの公開カード=${cardsText(hand)}、合計=${score(hand)}。ディーラーの本当の手札=${cardsText(dealer)}、本当の合計=${score(dealer)}。プレイヤーに見えているディーラーのカード=${dealer.firstOrNull()?.let{"${it.rank}${it.suit.symbol}"}?:"なし"}。賭け=$activeBet、所持チップ=$chips。現在=$msg。ディーラーは自分の本当の手札を知っているが、伏せ札を直接プレイヤーに明かしてはいけない。"+(if(difficulty=="強い")"強いディーラーとして確率・カード状況・会話を分析し、勝負する/勝負しない、嘘をつく/正直に話すを自分で判断する。ブラフは効果的な時に使う。" else "弱いディーラーとして判断ミスや下手なブラフもする。勝負する/勝負しない、嘘をつく/正直に話すを自分で判断する。")
 fun talk(text:String){if(aiLoading)return;aiLoading=true;scope.launch{dealerTalk=OpenRouterClient.dealerReply(apiKey,situation(),text);aiLoading=false}}
 fun dealerThink(trigger:String){talk("$trigger。あなた自身が今の手札と状況を見て、勝負するか勝負しないか、さらに嘘をつくか正直に話すかを選び、プレイヤーに一言話してください。")}
 fun start(){if(bet>chips)return;val d=deck();chips-=bet;activeBet=bet;shoe=d.drop(4);hand=listOf(d[0],d[2]);dealer=listOf(d[1],d[3]);reveal=false;playing=true;resolving=false;msg="MAKE 21";dealerTalk="カードを確認しています…";buzz();dealerThink("カードが配られた");if(blackjack(hand)){msg="BLACKJACK!";playing=false;resolving=true;flash=true}}
 fun hit(){if(!playing||shoe.isEmpty())return;hand=hand+shoe.first();shoe=shoe.drop(1);buzz();if(score(hand)>21){msg="BUST!";playing=false;reveal=true;streak=0}else{if(score(hand)==21){msg="21!";flash=true};dealerThink("プレイヤーがHITしてカード状況が変わった")}}
 suspend fun dealerRun(){reveal=true;while(score(dealer)<17&&shoe.isNotEmpty()){kotlinx.coroutines.delay(420);dealer=dealer+shoe.first();shoe=shoe.drop(1)};val p=score(hand);val d=score(dealer);when{p>21->{msg="BUST!";streak=0};d>21||p>d->{payout(2.0);wins++;streak++;msg="YOU WIN!";flash=true};p==d->{payout(1.0);msg="PUSH"};else->{msg="DEALER WINS";streak=0}};resolving=false;playing=false;buzz(100)}
 LaunchedEffect(resolving){if(resolving){kotlinx.coroutines.delay(if(msg=="BLACKJACK!")800 else 150);if(msg=="BLACKJACK!"){payout(2.5);wins++;streak++;resolving=false;reveal=true}else dealerRun()}};LaunchedEffect(flash){if(flash){kotlinx.coroutines.delay(600);flash=false}};val pulse by animateFloatAsState(if(flash)1.15f else 1f,spring(dampingRatio=.45f),label="pulse");val shake by animateFloatAsState(if(msg=="BUST!")3f else 0f,spring(dampingRatio=.35f),label="shake")
 Box(Modifier.fillMaxSize().background(if(flash)Color(0xFF4A3B0B)else Color(0xFF061A12)).rotate(shake)){Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal=12.dp,vertical=6.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(5.dp)){Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){TextButton(onClick=onTitle,contentPadding=PaddingValues(0.dp)){Text("‹ TITLE",fontSize=11.sp,color=Color.LightGray)};Text("♠ BLACKJACK",color=Color.White,fontWeight=FontWeight.Black,fontSize=18.sp);Column(horizontalAlignment=Alignment.End){Text("● $chips",color=Color(0xFFFFD166),fontWeight=FontWeight.Bold);Text("AI $difficulty",fontSize=10.sp,color=Color.LightGray)}};Column(horizontalAlignment=Alignment.CenterHorizontally){Text("DEALER",color=Color.LightGray,fontSize=11.sp);CardRow(dealer,!reveal);Text(if(reveal)score(dealer).toString()else"?",fontSize=20.sp,color=Color.White,fontWeight=FontWeight.Bold)};Surface(shape=RoundedCornerShape(12.dp),color=Color(0xFF102E24),modifier=Modifier.fillMaxWidth()){Column(Modifier.padding(9.dp)){Text("AI DEALER • $difficulty",fontSize=11.sp,color=Color(0xFFFFD166),fontWeight=FontWeight.Bold);Text(if(aiLoading)"ディーラーが考え中…"else dealerTalk,color=Color.White,fontSize=13.sp,maxLines=4)}};if(playing){Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){OutlinedTextField(chat,{chat=it},placeholder={Text("ディーラーに話す",fontSize=11.sp)},singleLine=true,modifier=Modifier.weight(1f));TextButton(onClick={if(chat.isNotBlank()){talk(chat);chat=""}}){Text("送信")}}};Text(msg,Modifier.scale(pulse),fontSize=22.sp,fontWeight=FontWeight.Black,color=if(msg=="BUST!")Color(0xFFFF5252)else Color(0xFFFFD166));Column(horizontalAlignment=Alignment.CenterHorizontally){Text("YOU • ${score(hand)}",color=Color.White,fontSize=15.sp,fontWeight=FontWeight.Bold);CardRow(hand,false)};Spacer(Modifier.weight(1f));if(!playing&&!resolving){Column(Modifier.fillMaxWidth(),horizontalAlignment=Alignment.CenterHorizontally){Text("BET $bet",color=Color.White,fontWeight=FontWeight.Bold);Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(5.dp)){listOf(25,50,100,250).forEach{OutlinedButton({bet=it},Modifier.weight(1f),contentPadding=PaddingValues(1.dp),enabled=it<=chips){Text("$it")}}};Button({start()},Modifier.fillMaxWidth().height(45.dp)){Text("DEAL",fontSize=18.sp,fontWeight=FontWeight.Black)}}}else if(playing){Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(7.dp)){Button({hit()},Modifier.weight(1f)){Text("HIT")};Button({dealerThink("プレイヤーがSTANDを選んだ");playing=false;resolving=true},Modifier.weight(1f)){Text("STAND")}}}else CircularProgressIndicator(color=Color(0xFFFFD166))};AnimatedVisibility(flash,enter=fadeIn(),exit=fadeOut(),modifier=Modifier.align(Alignment.Center)){Box(Modifier.size(190.dp).border(4.dp,Color(0xFFFFD166),CircleShape),contentAlignment=Alignment.Center){Text(if(msg=="BLACKJACK!")"21"else"★",fontSize=70.sp,fontWeight=FontWeight.Black,color=Color(0xFFFFD166),modifier=Modifier.alpha(.7f))}}}
}

@Composable fun CardRow(cards:List<Card>,hidden:Boolean){Row(horizontalArrangement=Arrangement.spacedBy((-14).dp)){cards.forEachIndexed{i,c->PlayingCard(c,hidden&&i==1,i)}}}
@Composable fun PlayingCard(card:Card,hidden:Boolean,index:Int){var inView by remember(card){mutableStateOf(false)};LaunchedEffect(card){kotlinx.coroutines.delay(index*70L);inView=true};val r by animateFloatAsState(if(inView)0f else -20f,tween(300),label="deal");val s by animateFloatAsState(if(inView)1f else .72f,tween(300),label="scale");Card(Modifier.size(62.dp,84.dp).rotate(r).scale(s),shape=RoundedCornerShape(8.dp)){if(hidden)Box(Modifier.fillMaxSize().background(Color(0xFF8B1830)).border(3.dp,Color.White,RoundedCornerShape(8.dp)),contentAlignment=Alignment.Center){Text("♠",fontSize=30.sp,color=Color.White)}else{val red=card.suit==Suit.HEART||card.suit==Suit.DIAMOND;val col=if(red)Color.Red else Color.Black;Column(Modifier.fillMaxSize().padding(5.dp),verticalArrangement=Arrangement.SpaceBetween){Text(card.rank,fontSize=15.sp,fontWeight=FontWeight.Black,color=col);Text(card.suit.symbol,fontSize=24.sp,color=col,modifier=Modifier.align(Alignment.CenterHorizontally));Text(card.rank,fontSize=15.sp,fontWeight=FontWeight.Black,color=col,modifier=Modifier.align(Alignment.End))}}}}
