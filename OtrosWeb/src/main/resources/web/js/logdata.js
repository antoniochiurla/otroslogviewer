var urlPrefix = "mock/";
urlPrefix = "";

var objDataStores = [];

function fillScene(){
	var geometryTimeline = new THREE.Geometry();
	geometryTimeline.vertices.push(
			new THREE.Vector3( 0, 0, 0),
			new THREE.Vector3( 0, 0, 1)
			);
//	var materialTimeline = new THREE.LineDashedMaterial( {
//		color: 0xffffff,
//		linewidth: 1,
//		scale: 1,
//		dashSize: 3,
//		gapSize: 1,
//	} );
	var materialTimeline = new THREE.LineBasicMaterial( {
		color: 0xffffff,
		linewidth: 1,
	} );
	
	var curveTimeStep = new THREE.EllipseCurve(
			0,  0,            // ax, aY
			200, 200,           // xRadius, yRadius
			0,  2 * Math.PI,  // aStartAngle, aEndAngle
			false,            // aClockwise
			0                 // aRotation
		);
	
	var geometryDataStore = new THREE.SphereGeometry( 40, 32, 32);
	var materialDataStore = new THREE.MeshLambertMaterial( { color: 0x00ff00 } );
	var geometryClazzes = new THREE.SphereGeometry( 30, 15, 15);
	var materialClazzes = new THREE.MeshLambertMaterial( { color: 0xff0000 } );
	var geometryClazz = new THREE.SphereGeometry( 10, 15, 15);
	var materialClazz = new THREE.MeshLambertMaterial( { color: 0xff0000 } );
	var geometryThreads = new THREE.SphereGeometry( 30, 15, 15);
	var materialThreads = new THREE.MeshLambertMaterial( { color: 0xff0000 } );
	var geometryThread = new THREE.SphereGeometry( 10, 15, 15);
	var materialThread = new THREE.MeshLambertMaterial( { color: 0xff0000 } );
	var geometryLogData = new THREE.SphereGeometry( 20, 15, 15);
	var materialLogData = new THREE.MeshLambertMaterial( { color: Math.random() * 0x0000ff } );
	getRemoteJSON(urlPrefix + "ds",function(jsonReply){
		  var minTime = jsonReply.ranges.time.min;
		  var maxTime = jsonReply.ranges.time.max;
		  var wideTime = maxTime - minTime;
		  var countDataStores = jsonReply.dataStores.length;
		  var circleDataStoresSize = 300 * ( countDataStores - 1 );
		  jsonReply.dataStores.forEach(function(ndxDataStore){
				var objDataStore = new THREE.Mesh( geometryDataStore, materialDataStore );
				objDataStore.clusterInfo = new ClusterInfo();
				objDataStore.position.x = Math.sin(ndxDataStore * 2 * Math.PI / countDataStores) * circleDataStoresSize;
				objDataStore.position.y = Math.cos(ndxDataStore * 2 * Math.PI / countDataStores) * circleDataStoresSize;
				objDataStore.position.z = 0;
				objDataStore.clusterInfo.xActual = objDataStore.position.x;
				objDataStore.clusterInfo.yActual = objDataStore.position.y;
				objDataStore.clusterInfo.zActual = objDataStore.position.z;
				objDataStore.objClasses = {};
				objDataStore.objThreads = {};
				scene.add( objDataStore );
				clusters.push( objDataStore );
				objects.push( objDataStore );
				objDataStores.push( objDataStore );
				getRemoteJSON(urlPrefix + "ds/" + ndxDataStore,function(jsonResponse){
				var stepIndex = 0;
				var stepsCount = jsonReply.ranges.time.steps.length;
				var countClasses = jsonResponse.classes.length;
				var countThreads = jsonResponse.threads.length;
					
				var objClazzes = new THREE.Mesh( geometryClazzes, materialClazzes );
				objClazzes.clusterInfo = new ClusterInfo();
				objClazzes.position.x = objDataStore.position.x + 100;
				objClazzes.position.y = objDataStore.position.y;
				objClazzes.position.z = 0;
				objClazzes.clusterInfo.xActual = objClazzes.position.x;
				objClazzes.clusterInfo.yActual = objClazzes.position.y;
				objClazzes.clusterInfo.zActual = objClazzes.position.z;
				objClazzes.objectName = "Classes";
				scene.add( objClazzes );
				clusters.push( objClazzes );
				objects.push( objClazzes );
				objDataStore.clusterInfo.addObject( objClazzes );
				
				var objThreads = new THREE.Mesh( geometryThreads, materialThreads );
				objThreads.clusterInfo = new ClusterInfo();
				objThreads.position.x = objDataStore.position.x - 100;
				objThreads.position.y = objDataStore.position.y;
				objThreads.position.z = 0;
				objThreads.clusterInfo.xActual = objThreads.position.x;
				objThreads.clusterInfo.yActual = objThreads.position.y;
				objThreads.clusterInfo.zActual = objThreads.position.z;
				objThreads.objectName = "Threads";
				scene.add( objThreads );
				clusters.push( objThreads );
				objects.push( objThreads );
				objDataStore.clusterInfo.addObject( objThreads );
				
				var stepInterval = jsonReply.ranges.time.steps[0];
				var time = minTime + stepInterval - ( minTime % stepInterval );
				while(time < maxTime){
					var color = new THREE.Color( 0, 0, 0 );
					var lineWidth;
					  for(stepIndex = 0; stepIndex < stepsCount; stepIndex++){
						  if( ( time % jsonReply.ranges.time.steps[ stepIndex ] ) == 0){
							  var tint = 0.7 - ( 0.1 * stepIndex );
							  color = new THREE.Color( tint, tint, tint );
							  lineWidth =  1 + stepIndex;
						  }
					  }
					  var materialTimestep;
					  materialTimestep = new THREE.LineBasicMaterial( {
						  color: color,
						  linewidth:  lineWidth
					  } );
					  var zPos = maxZ / wideTime * ( time - minTime );
					  
//					  var pointsTimeStep = curveTimeStep.getPoints( 20 ); // - stepIndex * 3 );
//					  var geometryTimeStep = new THREE.BufferGeometry().setFromPoints( pointsTimeStep );
//					  var circleStep = new THREE.Line( geometryTimeStep, materialTimestep );
//					  circleStep.position.x = objDataStore.position.x;
//					  circleStep.position.y = objDataStore.position.y;
//					  circleStep.position.z = zPos;
//					  circleStep.scale.x = 1; // - ( 0.1 * stepIndex );
//					  circleStep.scale.y = 1; // - ( 0.1 * stepIndex );
//					  circleStep.scale.z = 1;
//					  scene.add(circleStep);
//					  var stepDate = new Date(time);
//					  circleStep.objectName = stepDate.toISOString();
//					  objects.push(circleStep);
					  
					  var stepDate = new Date(time);
					  
					  console.debug("Time: " + time + " z: " + zPos);
					  

					  var geometryTimestep = new THREE.Geometry();
					  geometryTimestep.vertices.push(
							new THREE.Vector3( 0, 0, 0),
							new THREE.Vector3( 30 * countClasses, 0, 0)
					  );
					  var meshTimestep = new THREE.Line( geometryTimestep, materialTimestep);
					  meshTimestep.position.x = objClazzes.position.x;
					  meshTimestep.position.y = objClazzes.position.y;
					  meshTimestep.position.z = zPos + 30;
					  meshTimestep.objectName = stepDate.toISOString();
					  scene.add(meshTimestep);
					  objClazzes.clusterInfo.addObject( meshTimestep );

					  geometryTimestep = new THREE.Geometry();
					  geometryTimestep.vertices.push(
							new THREE.Vector3( 0, 0, 0),
							new THREE.Vector3( 0 - 30 * countThreads, 0, 0)
					  );
					  meshTimestep = new THREE.Line( geometryTimestep, materialTimestep);
					  meshTimestep.position.x = objThreads.position.x;
					  meshTimestep.position.y = objThreads.position.y;
					  meshTimestep.position.z = zPos + 30;
					  meshTimestep.scale.x = 1;
					  meshTimestep.scale.y = 1;
					  meshTimestep.scale.z = 1;
					  meshTimestep.objectName = stepDate.toISOString();
					  scene.add(meshTimestep);
					  objThreads.clusterInfo.addObject( meshTimestep );
					  
					  time += stepInterval;
				  }
					  var ndxClazz = 0;
					  jsonResponse.classes.forEach(function(clazz){
							 var objClazz = new THREE.Mesh( geometryClazz, materialClazz );
							 objClazz.position.x = objClazzes.position.x + ( ndxClazz + 1 ) * 30;
							 objClazz.position.y = objClazzes.position.y;
							 objClazz.position.z = objClazzes.position.z;
							 objClazz.clusterInfo = new ClusterInfo();
							 objClazz.clusterInfo.xActual = objClazz.position.x;
							 objClazz.clusterInfo.yActual = objClazz.position.y;
							 objClazz.clusterInfo.zActual = objClazz.position.z;
							 objClazz.objectName = clazz;
							 scene.add(objClazz);
							 objects.push( objClazz );
							 clusters.push( objClazz );
							 objClazzes.clusterInfo.addObject( objClazz );
							 objDataStore.objClasses[clazz] = objClazz;
							 
							  var meshTimeline = new THREE.Line( geometryTimeline, materialTimeline);
							  meshTimeline.position.x = objClazz.position.x;
							  meshTimeline.position.y = objClazz.position.y;
							  meshTimeline.position.z = objClazz.position.z;
							  meshTimeline.scale.x = 1;
							  meshTimeline.scale.y = 1;
							  meshTimeline.scale.z = maxZ;
							  meshTimeline.objectName = clazz;
							  scene.add(meshTimeline);
							  objClazz.clusterInfo.addObject( meshTimeline );

							 ndxClazz++;
					  });
					  var ndxThread = 0;
					  jsonResponse.threads.forEach(function(thread){
						  var objThread = new THREE.Mesh( geometryThread, materialThread );
						  objThread.position.x = objThreads.position.x - ( ndxThread + 1 ) * 30;
						  objThread.position.y = objThreads.position.y;
						  objThread.position.z = objThreads.position.z;
						  objThread.clusterInfo = new ClusterInfo();
						  objThread.clusterInfo.xActual = objThread.position.x;
						  objThread.clusterInfo.yActual = objThread.position.y;
						  objThread.clusterInfo.zActual = objThread.position.z;
						  objThread.objectName = thread;
						  scene.add(objThread);
						  objects.push( objThread );
						  clusters.push( objThread );
						  objThreads.clusterInfo.addObject( objThread );
						  objDataStore.objThreads[thread] = objThread;

						  var meshTimeline = new THREE.Line( geometryTimeline, materialTimeline);
						  meshTimeline.position.x = objThread.position.x;
						  meshTimeline.position.y = objThread.position.y;
						  meshTimeline.position.z = objThread.position.z;
						  meshTimeline.scale.x = 1;
						  meshTimeline.scale.y = 1;
						  meshTimeline.scale.z = maxZ;
						  meshTimeline.objectName = thread;
						  scene.add(meshTimeline);
						  objThread.clusterInfo.addObject( meshTimeline );

						  ndxThread++;
					  });
					  var ndxLogData = 0;
					  jsonResponse.logDatas.forEach(function(logData){
						  var col = logData.bg;
						  var color = new THREE.Color( col.r / 255, col.g / 255, col.b / 255);
						  var material = new THREE.MeshLambertMaterial( {
							  color: color,
							  transparent: true,
							  opacity: logData.numLevel / maxZ
							  } );
							 var zPos = maxZ / wideTime * ( logData.time - minTime ) + 30;
							 var yPos = 0; // ( logData.time % stepInterval ) / stepInterval * 300;
							 var objClass = objDataStore.objClasses[logData.clazz];
							 var objThread = objDataStore.objThreads[logData.thread];
							 [objThread, objClass].forEach(function(objParent){
							 
								 var object = new THREE.Mesh( geometryLogData, material );
								 object.position.x = objParent.position.x;
								 object.position.y = objParent.position.y + yPos;
								 object.position.z = objParent.position.z + zPos;
								
								 var dimension = 0.5 + ( logData.numLevel / 1000 );
								
								 object.scale.x = dimension;
								 object.scale.y = dimension;
								 object.scale.z = 0.2;
								
								 object.castShadow = true;
								 object.receiveShadow = true;
								
								 objParent.clusterInfo.addObject( object );
	
								 object.objectName = "-- loading --";
								 object.toBeLoaded = true;
								 object.dataIndex = ndxLogData;
								 object.dataStoreIndex = ndxDataStore;
								
								 scene.add( object );
								
								 objects.push( object );
							 });
							 ndxLogData++;
							 
					  });
				});
		  });
		});
}