try {
	var cluster = require('cluster');
	var os = require('os');
	var numCPUs = os.cpus().length;
	var net = require('net');
	var stdin = process.openStdin();
	var mongo = require('mongodb');
	var mongoClient = mongo.MongoClient;
	var ObjectId = require('mongodb').ObjectID;
	var http = require('http');
	var stream = require('stream').Transform;
	var crypto = require('crypto');
	var url = require("url");
} catch (error) {
	console.log("Couldn't load modules, type 'npm install' to install all dependencies");
	exitError();
}
const PORT = 2000, MANAGEMENT_PORT = 2001;
const VERSION = 1;
const DATABASE_URL = "mongodb://localhost:27017/doscientosymas";
const debug = process.argv.indexOf("-d") > -1;
const permisive = process.argv.indexOf("-p") > -1;
const singleThreaded = process.argv.indexOf("-s") > -1;
const completeDebug = process.argv.indexOf("-c") > -1;
const help = process.argv.indexOf("-h") > -1;
var db, HOST;

function exitError() {
	console.log("Server load stopped");
	process.exit();
}
if (help) {
	console.log("#200+ server help message:\n -d Show debug messages\n -p Run in permissive mode\n -s Run in single-threaded mode\n -c Run in complete debug mode\n -i Set default interface\n -h Show this message");
	process.exit();
}
if (cluster.isMaster) {
	function setupFunction(d) {
		if (d !== false) {
			HOST = selectable[parseInt(d.toString())];
			if (HOST == null) {
				console.log(parseInt(d.toString()) + " is not a valid option");
				return;
			}
		}
		stdin.on("data", function () {
			console.log("Cannot enter any command");
			return;
		});
		if (debug)
			console.log("Running in debug mode");
		if (permisive)
			console.log("Warning!! Running in permisive mode, server is vulnerable to attacks");
		if (singleThreaded)
			console.log("Running in signle-thread mode, performance may decrease");
		if (completeDebug)
			console.log("Running in complete debug mode");
		console.log("Loading server at IP " + HOST + "...");
		if (!debug) {
			process.stdout.write("<");
			animation = setInterval(function () {
					process.stdout.write("-");
				}, 20)
		};
		var ready = 0;
		for (var i = 0; i < (singleThreaded ? 1 : numCPUs); i++) {
			child = cluster.fork({
					"HOST": HOST
				});
			child.on('message', function (message) {
				if (message == "ready") {
					ready++;
					if (ready == numCPUs) {
						if (!debug) {
							clearInterval(animation);
							console.log(">");
						}
						console.log("All ready! =)");
					}
				} else if (message == "error_db") {
					clearInterval(animation);
					console.log(">");
					console.log("Couldn't connect to database, check mongodb is running on localhost and default port");
					exitError();
				}
			});
		}
		cluster.on('exit', (worker, code, signal) => {
			console.log(`Thread ${worker.process.pid} died`);
		});
		var httpserver = http.createServer(function handleRequest(request, response) {
				parsedurl = url.parse(request.url, true);
				params = parsedurl.query;
				if (debug)
					console.log("Requested http");
				if (request.headers.authorization != "Basic YWRtaW46eGRsb2w=") {
					response.writeHead(401, {
						'Content-Type': 'text/html;charset=utf-8',
						'WWW-Authenticate': 'Basic realm="management"'
					});
					response.end("Es necesaria la autentificación");
					return;
				}
				switch (parsedurl.pathname) {
				case "/":
					publishedrequested = parseInt(params.filter);
					if (params.filter == null) {
						filter = {};
						publishedrequested = -1;
					} else {
						filter = {
							publish_state: publishedrequested
						};
					}
					db.find(filter).sort({
						number: -1,
						_id: -1
					}).toArray(function (error, result) {
						if (error) {
							response.writeHead(500, {
								'Content-Type': 'text/html'
							});
							response.end('Ha habido un error al guardar los datos en la base de datos');
						} else {
							response.writeHead(200, {
								'Content-Type': 'text/html;charset=utf-8'
							});
							responsedata = '';
							if (publishedrequested == -1)
								responsedata += ('<span>Todo</span>&nbsp;');
							else
								responsedata += ('<a href="/">Todo</a>&nbsp;');
							if (publishedrequested == 0)
								responsedata += ('<span>Publicado</span>&nbsp;');
							else
								responsedata += ('<a href="/?filter=0">Publicado</a>&nbsp;');
							if (publishedrequested == 1)
								responsedata += ('<span>Pendiente de revisión</span>&nbsp;');
							else
								responsedata += ('<a href="/?filter=1">Pendiente de revisión</a>&nbsp;');
							if (publishedrequested == 2)
								responsedata += ('<span>Denegado</span>&nbsp;');
							else
								responsedata += ('<a href="/?filter=2">Denegado</a>&nbsp;');
							responsedata += ('</br><table><tr><th>id</th><th>número #</th><th>estado de la publicación</th><th>contenido</th><th>usuario</th><th>Likes</th></tr>');
							for (time = 0; time < result.length; time++) {
								responsedata += ('<tr><th>' + result[time]._id + '</th><th>' + result[time].number + '</th><th>' + (result[time].publish_state == 0 ? "publicado" : (result[time].publish_state == 2 ? "denegado" : "pendiente de revisión")) + '</th><th>' + result[time].content + '</th><th>' + result[time].user + '</th><th>' + result[time].likes.length + '</th><th><button onclick=\'location.href="/publish?id=' + result[time]._id + '"\'>Publicar</button></th><th><button onclick=\'location.href="/deny?id=' + result[time]._id + '"\'>Denegar</button></th><th><button onclick=\'location.href="/restore?id=' + result[time]._id + '"\'>Restablecer</button></th><!--<th><button onclick=\'location.href="/updatenumber?id=' + result[time]._id + '"\'>Actualizar número</button></th>--></tr>');
							}
							responsedata += '</table>';
							response.end(responsedata);
						}
					});
					break;
				case "/deny":
				case "/restore":
					db.update({
						_id: ObjectId(params.id)
					}, {
						$set: {
							publish_state: parsedurl.pathname == "/deny" ? 2 : 1,
							number: 2000000000
						}
					},
						function (error, result) {
						if (error) {
							response.writeHead(500, {
								'Content-Type': 'text/html'
							});
							response.end('Ha habido un error al guardar los datos en la base de datos');
						} else {
							response.writeHead(302, {
								'Content-Type': 'text/html',
								'Location': "/?filter=1" /*+ (parsedurl.pathname == "/publish" ? 0 : parsedurl.pathname == "/deny" ? 2 : 1)*/
							});
							response.end('');
						}
					});
					break;
				case "/publish":
					db.find({
						$and: [{
								number: {
									$lt: 2000000000
								}
							}, {
								publish_state: 0
							}
						]
					}).sort({
						number: -1
					}).toArray(function (error, result) {
						if (error) {
							response.writeHead(500, {
								'Content-Type': 'text/html'
							});
							response.end('Ha habido un error al guardar los datos en la base de datos');
						} else {
							db.update({
								_id: ObjectId(params.id)
							}, {
								$set: {
									number: result[0] != null ? result[0].number + 1 : 1,
									publish_state: 0
								}
							},
								function (error, result) {
								if (error) {
									response.writeHead(500, {
										'Content-Type': 'text/html'
									});
									response.end('Ha habido un error al guardar los datos en la base de datos');
								} else {
									response.writeHead(302, {
										'Content-Type': 'text/html',
										'Location': '/'
									});
									response.end('');
								}
							});
						}
					});
					break;
				}
			});
		mongoClient.connect(DATABASE_URL, function (error, dbresult) {
			if (error == null) {
				if (debug)
					console.log("Connected to database");
				db = dbresult.collection('records');
				httpserver.listen(MANAGEMENT_PORT, function () {
					if (debug)
						console.log("Management server listening on: %s", MANAGEMENT_PORT);
				});
			} else {
				throw "Couldn't connect to database";
			}
		});
	}

	interfaceposition = process.argv.indexOf("-i");
	showInfo = interfaceposition == -1;

	if (showInfo)
		console.log("Available network interfaces to run on:");
	inter = os.networkInterfaces();
	selectable = [];
	for (index in inter) {
		if (showInfo)
			console.log("\t" + index);
		for (vez = 0; vez < inter[index].length; vez++) {
			if (showInfo)
				console.log("\t\t" + selectable.length + ". " + inter[index][vez].address);
			selectable.push(inter[index][vez].address);
		}
	}

	if (!showInfo) {
		HOST = selectable[parseInt(process.argv[interfaceposition + 1])];
		setupFunction(false);
	} else {
		process.stdout.write("Enter number of IP to run server on from the list above:");
		stdin.on("data", setupFunction);
	}
} else {
	var HOST = process.env.HOST;
	mongoClient.connect(DATABASE_URL, function (error, db) {
		if (error == null) {
			if (debug)
				console.log("Connected to database");
			this.db = db.collection('records');
			server.listen(PORT, HOST);
		} else {
			if (debug) {
				console.log("Couldn't connect to database");
			} else {
				process.send("error_db");
			}
		}
	});
	const server = net.createServer(function (socket) {
			if (debug)
				console.log("New connection from " + socket.remoteAddress);
			sessionUserId = null;
			function send(answer) {
				if (debug)
					console.log("Sent " + answer.length + " bytes" + (completeDebug ? ": " + JSON.stringify(answer) : ""));
				socket.write(answer);
			}
			function processData(data) {
				try {
					if (debug)
						console.log("Proccess " + data.length + " bytes" + (completeDebug ? ": " + JSON.stringify(data) : ""));
					command = data.readUInt8(0);
					answer = null;
					switch (command) {
					case 0:
						limit = 2;
						answer = Buffer.from([0, data.readUInt8(1), VERSION]);
						break;
					case 1:
						limit = 103;
						userid = data.slice(3, 103).toString('utf8');
						if (data[2] == 1) {
							filter = [{
									publish_state: 0
								}
							];
						} else {
							filter = [{
									publish_state: 0
								}, {
									user_id: userid
								}
							]; ;
						}
						this.db.find({
							$or: filter
						}).sort({
							number: -1,
							_id: -1
						}).toArray(function (error, result) {
							try {
								if (error != null)
									throw new Error("Couldn't get from db: " + error);
								accessible = false;
								if (data[2] == 1) {
									result.sort(function (a, b) {
										return b.likes.length - a.likes.length;
									});
								}
								sendcontent = [];
								for (item in result) {
									item = result[item];
									sendcontent.push({
										number: item.number,
										publish_state: item.publish_state,
										likes: item.likes.length,
										content: item.content,
										_id: item._id,
										user: item.user,
										own_like: item.likes.includes(userid)
									});
								}
								sendData = JSON.stringify({
										"data": sendcontent
									});
								size = Buffer.byteLength(sendData);
								answer = Buffer.alloc(size + 6);
								answer[0] = 1;
								answer[1] = data[1];
								answer.writeUInt32BE(size, 2);
								answer.write(sendData, 6, size);
								send(answer);
							} catch (error) {
								handleError(error);
							}
						});
						break;
					case 2:
						limit = 2;
						socket.destroy();
						break;
					case 3:
						size = data.readUInt32BE(102);
						limit = 106 + size;
						contents = JSON.parse(data.slice(106, 106 + size).toString('utf8'));
						this.db.insert({
							number: 2000000000,
							publish_state: 1,
							content: contents.valor,
							user: contents.name,
							user_id: data.slice(2, 102).toString('utf8'),
							likes: []
						}, function (error, result) {
							try {
								if (error != null)
									throw new Error("Couldn't get from db: " + error);
								answer = Buffer.from([3, data.readUInt8(1)]);
								send(answer);
							} catch (error) {
								handleError(error);
							}
						});
						break;
					case 4:
						limit = 127;
						if (data[2] == 1) {
							updaterequest = {
								$addToSet: {
									likes: data.slice(3, 103).toString('utf8')
								}
							};
						} else {
							updaterequest = {
								$pull: {
									likes: data.slice(3, 103).toString('utf8')
								}
							};
						}
						this.db.update({
							_id: ObjectId(data.slice(103, 127).toString('utf8'))
						}, updaterequest);
						break;
					}
					if (answer != null) {
						send(answer);
					}
					if (data.length > limit) {
						processData(data.slice(limit));
					}
				} catch (error) {
					handleError(error);
				}
			}
			socket.on('data', function (data) {
				if (debug)
					console.log("Received " + data.length + " bytes" + (completeDebug ? ": " + JSON.stringify(data) : ""));
				processData(data);
			});
			socket.on('close', function (data) {
				if (debug)
					console.log("Client disconnected");
			});
			socket.on('error', function (error) {
				if (debug)
					console.log("Connection error");
			});
			socket.endError = function () {
				if (!permisive) {
					if (debug)
						console.log("Forced connection close due to illegal request")
						socket.end(Buffer.from([2, 1]));
					socket.destroy();
				} else {
					if (debug)
						console.log("Illegal request was made and ignored due to permisive mode");
				}
			}
			function handleError(error) {
				if (debug)
					console.error("Error catched", error);
				socket.endError();
			}
		}).on('listening', function () {
			process.send("ready");
		});
}
