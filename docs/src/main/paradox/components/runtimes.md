# Runtimes

__Runtime__ is a Docker image with a predefined infrastructure. It implements a set of specific methods that are used as an endpoints to the model. It's responsible for running user-defined models. When you create a new application you also have to provide a corresponding runtime to each models' instances.

We've already implemented a few runtimes which you can use in your own projects. They are all open-source and you can look up code if you need. 

<div class="flexible-table">
    <table>
        <thead>
            <tr>
                <th>Framework</th>
                <th>Runtime</th>
                <th>Links</th>
            </tr>
        </thead>
        <tbody>
            <tr>
                <td>Python</td>
                <td>hydrosphere/serving-runtime-python</td>
                <td>
                    <a href="https://hub.docker.com/r/hydrosphere/serving-runtime-python/">Docker Hub</a><br>
                    <a href="https://github.com/Hydrospheredata/hydro-serving-python">Github</a>
                </td>
            </tr>
            <tr>
                <td>Tensorflow</td>
                <td>hydrosphere/serving-runtime-tensorflow</td>
                <td>
                    <a href="https://hub.docker.com/r/hydrosphere/serving-runtime-tensorflow/">Docker Hub</a><br>
                    <a href="https://github.com/Hydrospheredata/hydro-serving-tensorflow">Github</a>
                </td>
            </tr>
            <tr>
                <td>Spark</td>
                <td>hydrosphere/serving-runtime-spark</td>
                <td>
                    <a href="https://hub.docker.com/r/hydrosphere/serving-runtime-spark/">Docker Hub</a><br>
                    <a href="https://github.com/Hydrospheredata/hydro-serving-spark">Github</a>
                </td>
            </tr>
        </tbody>
    </table>
</div>

<br>

@@@ note
If you are using a framework for which runtime isn't implemented yet, you can open an [issue][github-serving-new-issue] in our Github.
@@@



[github-serving-new-issue]: https://github.com/Hydrospheredata/hydro-serving/issues/new