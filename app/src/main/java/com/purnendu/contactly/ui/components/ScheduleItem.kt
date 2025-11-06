package com.purnendu.contactly.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.purnendu.contactly.R
import com.purnendu.contactly.model.Schedule
import com.purnendu.contactly.ui.theme.AntiFlashWhite
import com.purnendu.contactly.ui.theme.AuroMetalSaurus
import com.purnendu.contactly.ui.theme.ChineseBlack
import com.purnendu.contactly.ui.theme.ContactlyTheme

@Composable
fun ScheduleItem(
    schedule: Schedule,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ){

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 2.dp
            ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )
            {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                )
                {
                    // Contact info
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = schedule.name,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Original name: ${schedule.originalName}",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    }

                    // Avatar
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFFFAF0E6))
                    ) {
                        schedule.avatarResId?.let {
                            Image(
                                painter = painterResource(id = it),
                                contentDescription = "Avatar for ${schedule.name}",
                                modifier = Modifier.size(width = 119.dp,height = 58.dp),
                                contentScale = ContentScale.Crop
                            )
                        } ?: Image(
                            modifier = Modifier.size(width = 119.dp,height = 58.dp),
                            painter = painterResource(id =  R.drawable.avatar_placeholder),
                            contentDescription = "Avatar placeholder",
                            contentScale = ContentScale.Crop
                        )
                    }
                }




            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween)
        {
            Button(
                onClick = onDeleteClick,
                modifier = Modifier
                    .height(40.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AntiFlashWhite,
                    contentColor = ChineseBlack
                ),
                shape = RoundedCornerShape(8.dp),
            ) {
                Text("Delete", color = ChineseBlack, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = onEditClick,
                modifier = Modifier
                    .height(40.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AntiFlashWhite,
                    contentColor = ChineseBlack
                ),
                shape = RoundedCornerShape(8.dp),
            ) {
                Text("Edit",color = ChineseBlack, fontWeight = FontWeight.Bold)
            }
        }



    }


}

@Preview(showBackground = true)
@Composable
fun ScheduleItemPreview() {
    ContactlyTheme {
        ScheduleItem(
            schedule = Schedule(
                id = "1",
                name = "Ethan Carter",
                originalName = "Ethan"
            ),
            onEditClick = {},
            onDeleteClick = {}
        )
    }
}