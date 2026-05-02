<?php
error_reporting(E_ALL);
ini_set('display_errors', 1);

if ($_SERVER["REQUEST_METHOD"] == "POST") {
    include_once 'service/PositionService.php';
    create();
}

function create() {
    $latitude = $_POST['latitude'];
    $longitude = $_POST['longitude'];
    $datePosition = $_POST['date_position'];
    $imei = $_POST['imei'];

    $service = new PositionService();
    $position = new Position(null, $latitude, $longitude, $datePosition, $imei);
    $service->create($position);

    echo "Position enregistree avec succes";
}
?>