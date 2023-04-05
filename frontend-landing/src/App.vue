<script setup lang="ts">


import ExerciseOne from "@/ExerciseOne.vue";
import ExerciseTwo from "@/ExerciseTwo.vue";
</script>

<template>
    <div class="container-fluid">
        <nav class="navbar navbar-expand-lg navbar-light bg-white">
            <div class="container-fluid px-3">
                <a class="navbar-brand" href="/"><img src="./assets/logo.svg" alt="" width="30" height="24">Axon
                    Observability
                    training</a>
                <button class="navbar-toggler" type="button" data-bs-toggle="collapse"
                        data-bs-target="#navbarSupportedContent"
                        aria-controls="navbarSupportedContent" aria-expanded="false" aria-label="Toggle navigation">
                    <span class="navbar-toggler-icon"></span>
                </button>

                <ul class="navbar-nav me-auto mb-2 mb-lg-0">
                    <li class="nav-item">
                        <a class="nav-link active" aria-current="page" href="#">Home</a>
                    </li>
                    <li class="nav-item">
                        <a class="nav-link" target="_blank" href="/service-interfaces/">Auction interface</a>
                    </li>
                    <li class="nav-item dropdown">
                        <a class="nav-link dropdown-toggle" target="_blank" href="#" role="button"
                           data-bs-toggle="dropdown"
                           aria-expanded="false">
                            Firestarter
                        </a>
                        <ul class="dropdown-menu">
                            <li><a class="dropdown-item" target="_blank"
                                   href="/service-auctions/fire-starter/">Auctions</a></li>
                            <li><a class="dropdown-item" target="_blank" href="/service-auction-query/fire-starter/">Auction
                                Query</a></li>
                            <li><a class="dropdown-item" target="_blank"
                                   href="/service-auction-object-registry/fire-starter/">Auction Object
                                Registry</a></li>
                            <li><a class="dropdown-item" target="_blank" href="/service-participants/fire-starter/">Participants</a>
                            </li>
                        </ul>
                    </li>
                    <li class="nav-item">
                        <a class="nav-link" target="_blank" href="/axon-server/">Axon Server</a>
                    </li>
                    <li class="nav-item">
                        <a class="nav-link" target="_blank" href="/grafana/">Grafana</a>
                    </li>
                    <li class="nav-item">
                        <a class="nav-link" target="_blank" href="/jaeger/">Jaeger</a>
                    </li>
                </ul>

            </div>
        </nav>

        <div class="bg-white mt-4 p-4">
            <div class="row">
                <div class="col-md-5">
                    <h1 class="text-secondary">Welcome!</h1>
                    <p>You have arrived at the Axon Observability Training exercises application. It's time to put all
                        that theory
                        into practice and make sense of all metrics and data available to you.</p>

                    <p>
                        During this training you will observe a high-concurrency and scalable demo, so you can push Axon
                        Framework
                        and Server to its limits in a safe environment. This will give you a lot of experience, gained
                        in a safe
                        environments.
                        While doing so, please feel free to ask questions to your trainer.
                    </p>

                    <p>The top bar contains many useful links that you can click at any time. They will open a new
                        window.</p>
                    <ul>
                        <li><b>Auction Interface: </b> Here you can observe the high-concurrency demo in action. You can
                            also scale
                            its configuration there.
                        </li>
                        <li><b>FireStarter: </b> This section contains links to the FireStart of each server. Here you
                            can mess with
                            Axon's timings and error rate, in a true chaos way.
                        </li>
                        <li><b>Axon Server: </b> Access to the Axon Server dashboard. This demo runs Standard Edition to
                            prevent the
                            need for license management.
                        </li>
                        <li><b>Grafana: </b> Grafana can visualize metrics, host your dashboards and alert when stuff
                            goes wrong.
                            This is an important part of the training.
                        </li>
                        <li><b>Jaeger: </b> The Jaeger UI is used to access tracing. Feel free to poke around.</li>
                    </ul>

                    <div class="p-2">
                        <h2>Use Case</h2>
                        <p>
                            The Auction house hosts participants that enter a bidding war. They all try to make the most
                            money out of
                            the items they have by selling them, and buying others.
                        </p>
                        <p>
                            Each participant gets 3 items to start, and is free to start selling them at any time. Each
                            participant
                            listens to updates to auctions, and upon updates will decide whether they are interested and
                            if they enter
                            the bidding war. If so, each participant has an unique limit and will start bidding. Each
                            auction ends
                            after 10 seconds and the highest bid wins!
                        </p>

                        <p>
                            When an auction ends, the item that was auctioned is transferred by the Object Registry
                            service once the
                            balances have been settled.
                        </p>

                        <p>You can see all this visualized in the following diagram:</p>
                        <img class="p-2" src="./assets/sequence_overview.png" style="width: 100%">
                        <p>You can increase the number of participants, in the Configuration tab of the Auction
                            interface. Each
                            participant additionally will mean more auctions, and more bids per auction. Scale
                            wisely!</p>
                    </div>

                    <div class="p-2 mt-5">
                        <h2>Metric cheat sheet</h2>
                        <p>You can find out the metrics yourselves by exploring Grafana, or take the below cheatsheets from the presentation. Your choice.</p>
                        <h3>Commands & Queries (AF)</h3>

                        <table class="table table-responsive">
                            <tbody>
                            <tr>
                                <th>Latency</th>
                                <td>commandBus_successTimer_seconds</td>
                                <td>queryBus_successTimer_seconds</td>
                            </tr>
                            <tr>
                                <th>Traffic</th>
                                <td>commandBus_ingestedCounter_total</td>
                                <td>queryBus_ingestedCounter_total</td>
                            </tr>
                            <tr>
                                <th>Errors</th>
                                <td>commandBus_failureCounter_total</td>
                                <td>queryBus_failureCounter_total</td>
                            </tr>
                            <tr>
                                <th>Saturation</th>
                                <td>commandBus_capacity</td>
                                <td>queryBus_capacity</td>
                            </tr>
                            </tbody>
                        </table>

                        <h3>Commands (AS)</h3>

                        <table class="table table-responsive">
                            <tbody>
                            <tr>
                                <th>Latency</th>
                                <td>axon_commands_seconds_sum / axon_commands_seconds_count</td>
                                <td>axon_queries_seconds_sum / axon_queries_seconds_count</td>
                            </tr>
                            <tr>
                                <th>Traffic</th>
                                <td>rate(axon_commands_seconds_count)</td>
                                <td>rate(axon_queries_seconds_count)</td>
                            </tr>
                            <tr>
                                <th>Errors</th>
                                <td>-</td>
                                <td>-</td>
                            </tr>
                            <tr>
                                <th>Saturation</th>
                                <td>axon_commands_active</td>
                                <td>axon_queries_active</td>
                            </tr>
                            </tbody>
                        </table>

                        <h3>Event processors</h3>

                        <table class="table table-responsive">
                            <tbody>
                            <tr>
                                <th>Latency</th>
                                <td>eventProcessor_successTimer_seconds</td>
                            </tr>
                            <tr>
                                <th>Traffic</th>
                                <td>eventProcessor_ingestedCounter_total</td>
                            </tr>
                            <tr>
                                <th>Errors</th>
                                <td>eventProcessor_failureCounter_total</td>
                            </tr>
                            <tr>
                                <th>Saturation</th>
                                <td>eventProcessor_latency</td>
                            </tr>
                            </tbody>
                        </table>
                    </div>
                </div>
                <div class="col-md-7 pt-5">
                    <div class="shadow rounded p-3">
                        <h2 class="text-secondary">Exercise 1: Metrics</h2>
                        <ExerciseOne/>
                    </div>

                    <div class="shadow rounded p-3 mt-4">
                    <h2 class="text-secondary">Exercise 2: Alerts and Tracing</h2>
                    <ExerciseTwo/>
                    </div>
                </div>
            </div>


        </div>
    </div>
</template>

<style scoped>
.selling {
    color: orange;
}

.accordion-item {
    --bs-accordion-bg: transparent
}
</style>
