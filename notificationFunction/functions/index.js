

const functions =require('firebase-functions');
const admin =require('firebase-admin');
admin.initializeApp(functions.config().firebase);

exports.sendNotification = functions.database.ref('/notifications/{user_id}/{notification_id}').onWrite(event =>{

		const user_id = event.params.user_id;
		const notification_id = event.params.notification_id;

		console.log('We have a Notification to send to 	:', user_id);
		console.log('notification id:', notification_id);
		

		if(!event.data.val()){

			return console.log('new notification has been deleted from the databse : ', notification_id);
		}

		const fromUser = admin.database().ref(`/notifications/${user_id}/${notification_id}`).once('value');


		return fromUser.then(fromUserResult=>{

			const from_user_id = fromUserResult.val().from;

			console.log('you have notification from: ', from_user_id);

			const userQuery =  admin.database().ref(`Users/${from_user_id}/name`).once('value');
			return userQuery.then(userResult =>{
				const userName =userResult.val();

				const deviceToken = admin.database().ref(`/Users/${user_id}/device_token`).once('value');

		return deviceToken.then(result =>{

			const token_id = result.val();

			const payload = {
			notification: {
				title: "Friend request",
				body: `${userName} has sent you request`,
				icon: "default",
				click_action: "com.example.shrutihirpara.pingme_TARGET_NOTIFICATION"

			},
			data: {
				from_user_id: from_user_id
			}
		};

		return admin.messaging().sendToDevice(token_id, payload).then(response => {

			console.log('this was the notification feature');
		});



		});
			});


			
		});

		



	});
