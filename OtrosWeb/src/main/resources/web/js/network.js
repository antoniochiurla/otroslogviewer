class ClusterInfo {
   constructor() {
      this.objects = [];
   }
   addObject(object) {
      this.objects.push(object);
   }
   getObjects() {
      return this.objects;
   }
}

var container, stats;
var camera, controls, scene, renderer;
var objects = [];
var clusters = [];
var mouse = new THREE.Vector2();
var raycaster;
var sphereInter;
var objectInfoFlyDiv;
var objectInfoPersistDiv;
var maxZ = 5000;

init();
animate();

function init() {
	container = document.createElement( 'div' );
	document.body.appendChild( container );
	
	camera = new THREE.PerspectiveCamera( 70, window.innerWidth / window.innerHeight, 1, 10000 );
	camera.position.z = maxZ * 1.5;
	
	camera.position.x = 0;
	camera.position.y = -600;
	camera.position.z = 0;
	
//	controls = new THREE.TrackballControls( camera );
//	controls.rotateSpeed = 5.0;
//	controls.zoomSpeed = 2.0;
//	controls.panSpeed = 1.0;
//	controls.noZoom = false;
//	controls.noPan = false;
//	controls.staticMoving = true;
//	controls.dynamicDampingFactor = 0.3;
	
	controls = new THREE.OrbitControls( camera );
	controls.autoRotate = false;
	controls.minPolarAngle = 0;
	controls.maxPolarAngle = Math.PI;
	controls.minAzimuthAngle = 0 - Math.PI / 2;
	controls.maxAzimuthAngle = Math.PI / 2;
	
	scene = new THREE.Scene();
	scene.background = new THREE.Color( 0xf0f0f0 );
	
	scene.add( new THREE.AmbientLight( 0xffffff ) );
	
	var light = new THREE.SpotLight( 0xffffff, 1.5 );
	light.position.set( 0, 500, 2000 );
	light.angle = Math.PI / 9;
	
	light.castShadow = true;
	light.shadow.camera.near = 1000;
	light.shadow.camera.far = 4000;
	light.shadow.mapSize.width = 1024;
	light.shadow.mapSize.height = 1024;
	
//	scene.add( light );
	
	fillScene();

	renderer = new THREE.WebGLRenderer( { antialias: true } );
	renderer.setPixelRatio( window.devicePixelRatio );
	renderer.setSize( window.innerWidth, window.innerHeight );
	
	renderer.shadowMap.enabled = true;
	renderer.shadowMap.type = THREE.PCFShadowMap;
	
	container.appendChild( renderer.domElement );
	
	var dragControls = new THREE.DragControls( objects, camera, renderer.domElement );
	dragControls.addEventListener( 'dragstart', function ( event ) { controls.enabled = false; } );
	dragControls.addEventListener( 'drag', function ( event ) { moved(event); } );
	dragControls.addEventListener( 'dragend', function ( event ) { controls.enabled = true; } );
	
	
	var info = document.createElement( 'div' );
	info.style.position = 'absolute';
	info.style.top = '10px';
	info.style.width = '100%';
	info.style.textAlign = 'center';
	info.innerHTML = 'powered by <a href="http://threejs.org" target="_blank" rel="noopener">three.js</a>';
	
	objectInfoFlyDiv = document.createElement( 'div' );
	objectInfoFlyDiv.style.position = 'absolute';
	objectInfoFlyDiv.style.bottom = '10px';
	objectInfoFlyDiv.style.height = '10%';
	objectInfoFlyDiv.style.width = '30%';
	objectInfoFlyDiv.style.overflow = 'hidden';
	objectInfoFlyDiv.style.textAlign = 'left';
	objectInfoFlyDiv.innerHTML = '---';
	container.appendChild( objectInfoFlyDiv );
	
	objectInfoPersistDiv = document.createElement( 'div' );
	objectInfoPersistDiv.style.position = 'absolute';
	objectInfoPersistDiv.style.bottom = '10px';
	objectInfoPersistDiv.style.height = '10%';
	objectInfoPersistDiv.style.width = '70%';
	objectInfoPersistDiv.style.left = '30%';
	objectInfoPersistDiv.style.overflow = 'scroll';
	objectInfoPersistDiv.style.textAlign = 'left';
	objectInfoPersistDiv.innerHTML = '---';
	container.appendChild( objectInfoPersistDiv );
	
	stats = new Stats();
	container.appendChild( stats.dom );
	
	raycaster = new THREE.Raycaster();
	raycaster.linePrecision = 3;

	addSphereInter();
	
	window.addEventListener( 'resize', onWindowResize, false );
	window.addEventListener( 'mousemove', onWindowMouseMove, false );
	window.addEventListener( 'click', onWindowClick, false );
	window.addEventListener( 'touchstart', onWindowTouchStart, false );
	objectInfoPersistDiv.addEventListener( 'click', onPersistDivStopPropagation, false );
	objectInfoPersistDiv.addEventListener( 'wheel', onPersistDivStopPropagation, true );
	objectInfoPersistDiv.addEventListener( 'mousemove', onPersistDivStopPropagation, true );
//	objectInfoPersistDiv.addEventListener( 'touchstart', onPersistDivStopPropagation, true );

}

function addSphereInter() {
	var geometry = new THREE.SphereBufferGeometry( 5 );
	var material = new THREE.MeshBasicMaterial( { color: 0xff0000 } );
	sphereInter = new THREE.Mesh( geometry, material );
	sphereInter.visible = false;
	scene.add( sphereInter );
}

function moved( event ) {
  movedObject( event.object );
//  if(clusters.indexOf(event.object) != -1){
//	  var cluster = event.object;
//	  var info = cluster.clusterInfo;
//              var diffX = info.xActual - cluster.position.x;
//              var diffY = info.yActual - cluster.position.y;
//              var diffZ = info.zActual - cluster.position.z;
//              info.xActual = cluster.position.x;
//              info.yActual = cluster.position.y;
//              info.zActual = cluster.position.z;
//	  info.getObjects().forEach(function(object){ object.position.x -= diffX; object.position.y -= diffY; object.position.z -= diffZ; });
//  }
}

function movedObject(object){
	  if(clusters.indexOf(object) != -1){
		  var cluster = object;
		  var info = cluster.clusterInfo;
	              var diffX = info.xActual - cluster.position.x;
	              var diffY = info.yActual - cluster.position.y;
	              var diffZ = info.zActual - cluster.position.z;
	              info.xActual = cluster.position.x;
	              info.yActual = cluster.position.y;
	              info.zActual = cluster.position.z;
		  info.getObjects().forEach(
				  function(object){
					  object.position.x -= diffX;
					  object.position.y -= diffY;
					  object.position.z -= diffZ;
					  movedObject( object );
				  }
		  );
	  }
}

function onWindowMouseMove( event ) {
	event.preventDefault();
	mouse.x = ( event.clientX / window.innerWidth ) * 2 - 1;
	mouse.y = - ( event.clientY / window.innerHeight ) * 2 + 1;
}

function onWindowClick( event ) {
	if(event.button == 0){
		selectObjectUnderMouse(objectInfoPersistDiv,true);
	}
}

function onWindowTouchStart( event ) {
	event.preventDefault();
	event.clientX = event.touches[0].clientX;
	event.clientY = event.touches[0].clientY;
	onDocumentMouseDown( event );
}

function onPersistDivStopPropagation( event ) {
	event.stopPropagation();
}

function onWindowResize() {

    camera.aspect = window.innerWidth / window.innerHeight;
    camera.updateProjectionMatrix();

    renderer.setSize( window.innerWidth, window.innerHeight );

}

function animate() {

    setTimeout( function() {

        requestAnimationFrame( animate );

    }, 1000 / 10 );
    // requestAnimationFrame( animate );

    render();
    stats.update();

}

function render() {

	selectObjectUnderMouse(objectInfoFlyDiv);

    controls.update();

    renderer.render( scene, camera );
    
    console.log("Camera "
    		+ "pos: " + camera.position.x + "," + camera.position.y + "," + camera.position.z
    		+ " rot: " + camera.rotation.x + "," + camera.rotation.y + "," + camera.rotation.z
    		);

}

function selectObjectUnderMouse(div,sendSelection) {
	raycaster.setFromCamera( mouse, camera );
	var intersects = raycaster.intersectObjects( scene.children, true);
	var selected = 0;
	var text = "";
	if(mouse.x == 0 && mouse.y == 0){
		return;
	}
	// console.log("Intersect: " + intersects.length + " mouse: " + mouse.x + "," + mouse.y);
	var lastDataStoreIndex;
	var lastLogDataIndex;
	for(var index = 0; index < intersects.length; index++){
		var currentIntersected = intersects[ index ].object;
		// sphereInter.visible = true;
		sphereInter.position.copy( intersects[ index ].point );
		if(typeof(currentIntersected.objectName) !== 'undefined'){
			lastDataStoreIndex = currentIntersected.dataStoreIndex;
			lastLogDataIndex = currentIntersected.dataIndex;
			if(currentIntersected.toBeLoaded){
				currentIntersected.toBeLoaded = false;
				getRemoteJSON(urlPrefix + "ds/" + lastDataStoreIndex + "/" + lastLogDataIndex,function(jsonResponse,object){
					var logData = jsonResponse.logDatas[ 0 ];
					var timeFormatted = new Date(logData.time).toISOString()
					var name = timeFormatted + " " + logData.level + " " + logData.clazz + " " + logData.message;
					object.objectName = name;
					object.toBeLoaded = false;
				},currentIntersected);
				text = text + "loading" + "<BR/>";
			} else {
				text = text + currentIntersected.objectName + "<BR/>";
			}
			selected++;
			// break;
		} else {
			// text = text + currentIntersected + "<BR/>";
		}
	}
	if(sendSelection && selected > 0){
		getRemoteJSON(urlPrefix + "ds/" + lastDataStoreIndex + "/sel/" + lastLogDataIndex,function(jsonResponse){});
	}
	if(selected > 0){
		text = selected + "<BR/>" + text;
	}
	div.innerHTML = text;
	if( selected == 0 ){
		currentIntersected = undefined;
		sphereInter.visible = false;
		div.innerHTML = "-- unselected --";
	}
}

function getRemoteJSON(url, callbackFunction, object){
	var xmlhttp = new XMLHttpRequest();

	xmlhttp.onreadystatechange = function() {
		if (this.readyState == 4 && this.status == 200) {
		    var myArr = JSON.parse(this.responseText);
		    callbackFunction(myArr,object);
	    }
	};

	xmlhttp.open("GET", url, true);
	xmlhttp.send(); 
}