/*
Server code for #200+
*/
try{
	var cluster = require('cluster');
	var os = require('os');
	var numCPUs = os.cpus().length;
	var net = require('net');
	var stdin = process.openStdin();
	var mongo = require('mongodb');
	var mongoClient = mongo.MongoClient;
	var https = require('https');
	var stream = require('stream').Transform;
	var crypto = require('crypto');
}catch(error){
	console.log("Couldn't load modules, type 'npm install' to install all dependencies");
	exitError();
}

const PORT = 2000;//Using 1234 temporary
//const HOST = os.networkInterfaces()["Wi-Fi"][1].address;//Local IP of my computer on my home network, may change depending on OS and wired/wifi connection
const VERSION = 1;
const DATABASE_URL = "mongodb://localhost:27017/doscientosymas";//URL to mongoDB database
const debug = process.argv.indexOf("-d")>-1;
const permisive = process.argv.indexOf("-p")>-1;
const singleThreaded = process.argv.indexOf("-s")>-1;
const completeDebug = process.argv.indexOf("-c")>-1;
const help = process.argv.indexOf("-h")>-1;
var db;

function exitError(){
	console.log("Server load stopped");
	process.exit();
}

if(help){
	console.log("#200+ server help message:\n -d Show debug messages\n -p Run in permissive mode\n -s Run in single-threaded mode\n -c Run in complete debug mode\n -h Show this message");
	process.exit();
}

if (cluster.isMaster) {
	console.log("Available network interfaces to run on:");
	inter = os.networkInterfaces();
	selectable = [];
	for(index in inter) {
		console.log("\t"+index);
		for(vez=0;vez<inter[index].length;vez++){
			console.log("\t\t"+selectable.length+". "+inter[index][vez].address);
			selectable.push(inter[index][vez].address);
		}
	}
	process.stdout.write("Enter number of IP to run server on from the list above:");

	stdin.on("data", function(d) {
		stdin.on("data",function(){console.log("Cannot enter any command");});
		var HOST = selectable[d.toString().trim()];
		//HOST = 1;
		if(HOST == null){
			console.log(d.toString().trim()+" is not a valid option");
			return;
			exitError();
		}
		if(debug)console.log("Running in debug mode");
		if(permisive)console.log("Warning!! Running in permisive mode, server is vulnerable to attacks");
		if(singleThreaded)console.log("Running in signle-thread mode, performance may decrease");
		if(completeDebug)console.log("Running in complete debug mode");
		console.log("Loading server at IP "+HOST+"...");
		if(!debug){process.stdout.write("<");animation = setInterval(function(){process.stdout.write("-");},20)};
		
		var ready = 0;
		for (var i = 0; i < (singleThreaded?1:numCPUs); i++) {// Create as much threads as cores of the cpu to distribute load
			child = cluster.fork({"HOST":HOST});
			child.on('message',function(message){
				if(message == "ready"){
					ready++;
					if(ready == numCPUs){
						if(!debug){clearInterval(animation);console.log(">");}
						console.log("All ready! =)");
					}
				}else if(message == "error_db"){
					clearInterval(animation);console.log(">");
					console.log("Couldn't connect to database, check mongodb is running on localhost and default port");
					exitError();
				}
			});
		}

		cluster.on('exit', (worker, code, signal) => {
			console.log(`Thread ${worker.process.pid} died`);
		});
	});
} else {
	var HOST = process.env.HOST;
	// Connect with database
	mongoClient.connect(DATABASE_URL, function(error, db) {
		if(error == null){
			if(debug)console.log("Connected to database");
			this.db = db.collection('records');
			//console.log(this.db);
			server.listen(PORT, HOST);// Make server listen when db is ready
		}else{
			if(debug){
				console.log("Couldn't connect to database");
			}
			else{
				process.send("error_db");
			}
		}
	});

	// Create TCP server on each thread
	const server = net.createServer(function(socket) {
		
		if(debug)console.log("New connection from "+socket.remoteAddress);
		
		//sessionSub = null;
		sessionUserId = null;
		
		function send(answer){
			if(debug)console.log("Sent "+answer.length+" bytes"+(completeDebug?": "+JSON.stringify(answer):""));
			socket.write(answer);
		}
		
		function processData(data){
			try{
				if(debug)
				console.log("Proccess "+data.length+" bytes"+(completeDebug?": "+JSON.stringify(data):""));
				command = data.readUInt8(0);
				answer = null;
				switch(command){
				case 0://Info request
					limit = 2;
					//if(data.length > limit){processData(data.slice(limit));}
					answer = Buffer.from([0,data.readUInt8(1),VERSION]);//Answer requested command, request ID and VERSION
					break;
				case 1://Info request
					limit = 102;
					this.db.find({$or:[{publish_state:0},{user_id:data.slice(2,102).toString('utf8')}]}).sort({number:-1,_id:-1}).toArray(function(error,result){
						try{
							if(error != null)throw new Error("Couldn't get from db: "+error);
							accessible = false;
							sendData = JSON.stringify({"data":result});
							size = Buffer.byteLength(sendData);
							
							answer = Buffer.alloc(size+6);
							answer[0] = 1;
							answer[1] = data[1];
							answer.writeUInt32BE(size,2);
							answer.write(sendData,6,size);
							
							send(answer);
						}catch(error){handleError(error);}
					});
					break;
				case 2:
					limit = 2;
					socket.destroy();
					break;
				case 3:
					size = data.readUInt32BE(102);
					limit = 106+size;
					contents = JSON.parse(data.slice(106,106+size).toString('utf8'));
					this.db.insert({number:2000000000,publish_state:1,content:contents.valor,user:contents.name,user_id:data.slice(2,102).toString('utf8')},function(error,result){
						try{
							if(error != null)throw new Error("Couldn't get from db: "+error);
							answer = Buffer.from([3,data.readUInt8(1)]);
							send(answer);
						}catch(error){handleError(error);}
						});
					//this.db.insertOne({number:0,publish_state:1,content,user,user_id:});
					break;
				}
				
				if(answer != null){send(answer);}
				if(data.length > limit){processData(data.slice(limit));}//There are more request on the data received so process them
				
			}catch(error){
				handleError(error);
			}
		}
		
		socket.on('data', function(data) {
			if(debug)console.log("Received "+data.length+" bytes"+(completeDebug?": "+JSON.stringify(data):""));
			processData(data);
		});
		
		socket.on('close', function(data) {
			if(debug)console.log("Client disconnected");
		});
		
		socket.on('error',function(error){
			if(debug)console.log("Connection error");
		});
		
		socket.endError = function(){
			if(!permisive){
				if(debug)console.log("Forced connection close due to illegal request")
				socket.end(Buffer.from([2,1]));
				socket.destroy();
			}else{
				if(debug)console.log("Illegal request was made and ignored due to permisive mode");
			}
		}
		
		function handleError(error){
			if(debug)console.error("Error catched",error);
			socket.endError();
		}
		
	}).on('listening',function(){process.send("ready");/*Tell main thread we are ready/*console.log("Ready - "+cluster.worker.id);*/});
}