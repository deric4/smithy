===============================
Amazon API Gateway Integrations
===============================

Smithy can integrate with Amazon API Gateway using traits, authentication
schemes, and OpenAPI specifications.

.. contents:: Table of contents
    :depth: 2
    :local:
    :backlinks: none

-------------------------
Amazon API Gateway traits
-------------------------

.. _aws.apigateway#apiKeySource-trait:

``aws.apigateway#apiKeySource`` trait
=====================================

Summary
    Specifies the source of the caller identifier that will be used to
    throttle API methods that require a key.
Trait selector
    ``service``
Value type
    ``string`` set to one of the following values:

    .. list-table::
        :header-rows: 1
        :widths: 20 80

        * - Value
          - Description
        * - HEADER
          - for receiving the API key from the X-API-Key header of a request
        * - AUTHORIZER
          - for receiving the API key from the UsageIdentifierKey
            from a Lambda authorizer (formerly known as a custom authorizer)
See also
    - `Create and Use Usage Plans with API Keys`_ for more on usage plans and
      API keys
    - `Choose an API Key Source`_ for information on choosing a source
    - `x-amazon-apigateway-api-key-source`_ for how this relates to OpenAPI

The following example sets the ``X-API-Key`` header as the API key source.

.. tabs::

    .. code-tab:: smithy

        $version: "0.4.0"
        namespace smithy.example

        use aws.apigateway#apiKeySource

        @apiKeySource("HEADER")
        service Weather {
            version: "2018-03-17"
        }

    .. code-tab:: json

        {
            "smithy": "0.4.0",
            "smithy.example": {
                "shapes": {
                    "Weather": {
                        "type": "service",
                        "version": "2018-03-17",
                        "aws.apigateway#apiKeySource": "HEADER"
                    }
                }
            }
        }

.. note::

    This trait should be considered internal-only and not exposed to your
    customers.


.. _aws.apigateway#authorizers-trait:

``aws.apigateway#authorizers`` trait
====================================

Summary
    `Lambda authorizers`_ to attach to the authentication schemes defined on
    this service.
Trait selector
    ``service[trait|protocols]``

    *A service shape that has the protocols trait*
Value type
    Map of arbitrary names to *authorizer* definitions. These authorizer
    definitions are applied to a service, resource, or operation using the
    :ref:`aws.apigateway#authorizer-trait`.

An *authorizer* definition is an object that supports the following properties:

.. list-table::
    :header-rows: 1
    :widths: 10 20 70

    * - Property
      - Type
      - Description
    * - type
      - ``string``
      - **Required**. The type of the authorizer. This is a required property
        and the value must be "token", for an authorizer with the caller
        identity embedded in an authorization token, or "request", for an
        authorizer with the caller identity contained in request parameters.
    * - scheme
      - ``string``
      - **Required**. A Smithy authentication scheme name that identifies how
        a client authenticates. This value MUST reference one of the ``auth``
        schemes of the :ref:`protocols-trait` attached to the service.
    * - uri
      - ``string``
      - **Required.** Specifies the authorizer's Uniform Resource Identifier
        (URI). For ``token`` or ``request`` authorizers, this must be a
        well-formed Lambda function URI, for example,
        ``arn:aws:apigateway:us-west-2:lambda:path/2015-03-31/functions/arn:aws:lambda:us-west-2:{account_id}:function:{lambda_function_name}/invocations``.
        In general, the URI has this form ``arn:aws:apigateway:{region}:lambda:path/{service_api}``,
        where ``{region}`` is the same as the region hosting the Lambda
        function, path indicates that the remaining substring in the URI
        should be treated as the path to the resource, including the initial
        ``/``. For Lambda functions, this is usually of the form
        ``/2015-03-31/functions/[FunctionARN]/invocations``.
    * - credentials
      - ``string``
      - Specifies the required credentials as an IAM role for API Gateway to
        invoke the authorizer. To specify an IAM role for API Gateway to
        assume, use the role's Amazon Resource Name (ARN). This value MUST
        be omitted in order to use resource-based permissions on the
        Lambda function.
    * - identitySource
      - ``string``
      - The identity source for which authorization is requested.

        For a ``token`` or ``cognito_user_pools`` authorizer, this is required
        and specifies the request header mapping expression for the custom
        header holding the authorization token submitted by the client. For
        example, if the token header name is Auth, the header mapping
        expression is ``method.request.header.Auth``.

        For the ``request`` authorizer, this is required when authorization
        caching is enabled. The value is a comma-separated string of one or
        more mapping expressions of the specified request parameters. For
        example, if an Auth header and a Name query string parameter are
        defined as identity sources, this value is ``method.request.header.Auth, method.request.querystring.Name``.
        These parameters will be used to derive the authorization caching
        key and to perform runtime validation of the ``request`` authorizer
        by verifying all of the identity-related request parameters are
        present, not null and non-empty. Only when this is true does the
        authorizer invoke the authorizer Lambda function, otherwise, it
        returns a 401 Unauthorized response without calling the Lambda
        function. The valid value is a string of comma-separated mapping
        expressions of the specified request parameters. When the
        authorization caching is not enabled, this property is optional.
    * - identityValidationExpression
      - ``string``
      - A validation expression for the incoming identity token. For ``token``
        authorizers, this value is a regular expression. API Gateway will
        match the aud field of the incoming token from the client against
        the specified regular expression. It will invoke the authorizer's
        Lambda function when there is a match. Otherwise, it will return a
        401 Unauthorized response without calling the Lambda function. The
        validation expression does not apply to the ``request`` authorizer.
    * - resultTtlInSeconds
      - ``integer``
      - The TTL in seconds of cached authorizer results. If it equals 0,
        authorization caching is disabled. If it is greater than 0,
        API Gateway will cache authorizer responses. If this field is not set,
        the default value is 300. The maximum value is 3600, or 1 hour.

..
    TODO: Add IDL example

.. code-block:: json

    {
        "smithy": "0.4.0",
        "ns.foo": {
            "shapes": {
                "Weather": {
                    "type": "service",
                    "version": "2018-03-17",
                    "protocols": [
                        {
                            "name": "aws.rest-json",
                            "auth": ["aws.v4"]
                        }
                    ],
                    "aws.apigateway#authorizer": "arbitrary-name",
                    "aws.apigateway#authorizers": {
                        "arbitrary-name": {
                            "scheme": "aws.v4",
                            "type": "request",
                            "uri": "arn:foo:baz",
                            "credentials": "arn:foo:bar",
                            "identitySource": "mapping.expression",
                            "identityValidationExpression": "[A-Z]+",
                            "resultTtlInSeconds":100
                        }
                    }
                }
            }
        }
    }

.. note::

    This trait should be considered internal-only and not exposed to your
    customers.


.. _aws.apigateway#authorizer-trait:

``aws.apigateway#authorizer`` trait
====================================

Summary
    Applies a Lambda authorizer to a service, resource, or operation.
    Authorizers are resolved hierarchically: an operation inherits
    the effective authorizer applied to a parent resource or operation.
Trait selector
    ``:each(service, resource, operation)``

    *A service, resource, or operation*
Value type
    String value that MUST reference one of the keys in the
    :ref:`aws.apigateway#authorizers-trait` of the service that contains
    the shape.

.. note::

    This trait should be considered internal-only and not exposed to your
    customers.


.. _aws.apigateway#requestValidator-trait:

``aws.apigateway#requestValidator`` trait
=========================================

Summary
    Opts-in to Amazon API Gateway request validation for a service or
    operation.
Trait selector
    ``:test(service, operation)``
Value type
    ``string`` value set to one of the following:

    .. list-table::
        :header-rows: 1
        :widths: 20 80

        * - Value
          - Description
        * - full
          - The parameters and body of a request are validated.
        * - params-only
          - Only the parameters of a request are validated.
        * - body-only
          - Only the body of a request is validated.
See also
    - `Enable Request Validation in API Gateway`_ for more information
    - :ref:`apigateway-request-validators` for information on how this converts
      to OpenAPI
    - `x-amazon-apigateway-request-validator`_ for more on how this converts
      to OpenAPI
    - `x-amazon-apigateway-request-validators`_ for more on how this converts
      to OpenAPI

Then following example enables request validation on a service:

.. tabs::

    .. code-tab:: smithy

        $version: "0.4.0"
        namespace smithy.example

        use aws.apigateway#requestValidator

        @requestValidator("full")
        service Weather {
            version: "2018-03-17"
        }

    .. code-tab:: json

        {
            "smithy": "0.4.0",
            "smithy.example": {
                "shapes": {
                    "Weather": {
                        "type": "service",
                        "version": "2018-03-17",
                        "aws.apigateway#requestValidator": "full"
                    }
                }
            }
        }

.. note::

    This trait should be considered internal-only and not exposed to your
    customers.


.. _aws.apigateway#integration-trait:

``aws.apigateway#integration`` trait
====================================

Summary
    Defines an `API Gateway integration`_ that integrates with an actual
    backend.
Trait selector
    ``:test(service, operation)``
Value type
    ``object`` value.
See also
    - :ref:`apigateway-integrations` for information on how this converts
      to OpenAPI
    - `API Gateway Integration`_ for in-depth API documentation
    - `x-amazon-apigateway-integration`_ for details on how this looks
      to OpenAPI

The ``aws.apigateway#integration`` trait is an object that supports the
following properties:

.. list-table::
    :header-rows: 1
    :widths: 10 20 70

    * - Property
      - Type
      - Description
    * - type
      - ``string``
      - **Required.** The type of integration with the specified backend.
        Valid values are:

        - ``http`` or ``http_proxy``: for integration with an HTTP backend
        - ``aws_proxy``: for integration with AWS Lambda functions
        - ``aws``: for integration with AWS Lambda functions or other AWS
          services such as Amazon DynamoDB, Amazon Simple Notification Service
          or Amazon Simple Queue Service.
    * - uri
      - ``string``
      - **Required.** The endpoint URI of the backend. For integrations of
        the ``aws`` type, this is an ARN value. For the HTTP integration,
        this is the URL of the HTTP endpoint including the ``https`` or
        ``http`` scheme.
    * - httpMethod
      - ``string``
      - **Required.** Specifies the integration's HTTP method type
        (for example, ``POST``). For Lambda function invocations, the value
        must be ``POST``.
    * - credentials
      - ``string``
      - Specifies the credentials required for the integration, if any. For
        AWS IAM role-based credentials, specify the ARN of an appropriate
        IAM role. If unspecified, credentials will default to resource-based
        permissions that must be added manually to allow the API to access
        the resource. For more information, see
        `Granting Permissions Using a Resource Policy`_.
    * - passThroughBehavior
      - ``string``
      - Specifies how a request payload of unmapped content type is passed
        through the integration request without modification. Supported
        values are ``when_no_templates``, ``when_no_match``, and ``never``.
        For more information, see `Integration.passthroughBehavior`_.
    * - contentHandling
      - :ref:`ContentHandling string <apigateway-content-handling>`
      - Request payload content handling.
    * - timeoutInMillis
      - ``integer``
      - Integration timeouts between 50 ms and 29,000 ms.
    * - connectionId
      - ``string``
      - The ID of a `VpcLink`_ for the private integration.
    * - connectionType
      - ``string``
      - The type of the network connection to the integration endpoint.
        The valid value is ``INTERNET`` for connections through the public
        routable internet or ``VPC_LINK`` for private connections between
        API Gateway and a network load balancer in a VPC. The default
        value is ``INTERNET``.
    * - cacheNamespace
      - ``string``
      - An API-specific tag group of related cached parameters.
    * - cacheKeyParameters
      - ``[string]``
      - A list of request parameter names whose values are to be cached.
    * - requestParameters
      - ``Map`` of :ref:`apigateway-requestParameters` to request parameters
      - Specifies mappings from method request parameters to integration
        request parameters. Supported request parameters are querystring,
        path, header, and body.
    * - requestTemplates
      - ``Map`` of media types to :ref:`apigateway-requestTemplates`
      - Mapping templates for a request payload of specified media types.
    * - responses
      - ``Map`` of response codes to :ref:`apigateway-responses`
      - Defines the method's responses and specifies desired parameter
        mappings or payload mappings from integration responses to method
        responses.

The following example defines an integration that is applied to every
operation within the service.

..
    TODO: Add Smithy example

.. code-block:: json

    {
        "smithy": "0.4.0",
        "smithy.example": {
            "shapes": {
                "Weather": {
                    "type": "service",
                    "version": "2018-03-17",
                    "protocols": [{"name": "aws.rest-json", "auth": ["aws.v4"]}],
                    "aws.apigateway#integration": {
                        "type": "aws",
                        "uri" : "arn:aws:apigateway:us-east-1:lambda:path/2015-03-31/functions/arn:aws:lambda:us-east-1:012345678901:function:HelloWorld/invocations",
                        "httpMethod" : "POST",
                        "credentials" : "arn:aws:iam::012345678901:role/apigateway-invoke-lambda-exec-role",
                        "requestTemplates" : {
                            "application/json" : "#set ($root=$input.path('$')) { \"stage\": \"$root.name\", \"user-id\": \"$root.key\" }",
                            "application/xml" : "#set ($root=$input.path('$')) <stage>$root.name</stage> "
                        },
                        "requestParameters" : {
                            "integration.request.path.stage" : "method.request.querystring.version",
                            "integration.request.querystring.provider" : "method.request.querystring.vendor"
                        },
                        "cacheNamespace" : "cache namespace",
                        "cacheKeyParameters" : [],
                        "responses" : {
                            "2\\d{2}" : {
                                "statusCode" : "200",
                                "responseParameters" : {
                                    "method.response.header.requestId" : "integration.response.header.cid"
                                },
                                "responseTemplates" : {
                                    "application/json" : "#set ($root=$input.path('$')) { \"stage\": \"$root.name\", \"user-id\": \"$root.key\" }",
                                    "application/xml" : "#set ($root=$input.path('$')) <stage>$root.name</stage> "
                                }
                            },
                            "302" : {
                                "statusCode" : "302",
                                "responseParameters" : {
                                    "method.response.header.Location" : "integration.response.body.redirect.url"
                                }
                            },
                            "default" : {
                                "statusCode" : "400",
                                "responseParameters" : {
                                    "method.response.header.test-method-response-header" : "'static value'"
                                }
                            }
                        }
                    }
                }
            }
        }
    }

.. note::

    This trait should be considered internal-only and not exposed to your
    customers.


.. _aws.apigateway#mockIntegration-trait:

``aws.apigateway#mockIntegration`` trait
========================================

Summary
    Defines an `API Gateway integration`_ that returns a mock response.
Trait selector
    ``:test(service, operation)``
Value type
    ``object`` value.

.. list-table::
    :header-rows: 1
    :widths: 10 20 70

    * - Property
      - Type
      - Description
    * - passThroughBehavior
      - ``string``
      - Specifies how a request payload of unmapped content type is passed
        through the integration request without modification. Supported
        values are ``when_no_templates``, ``when_no_match``, and ``never``.
        For more information, see `Integration.passthroughBehavior`_.
    * - requestParameters
      - ``Map`` of :ref:`apigateway-requestParameters` to request parameters
      - Specifies mappings from method request parameters to integration
        request parameters. Supported request parameters are querystring,
        path, header, and body.
    * - requestTemplates
      - ``Map`` of media types to :ref:`apigateway-requestTemplates`
      - Mapping templates for a request payload of specified media types.
    * - responses
      - ``Map`` of response codes to :ref:`apigateway-responses`
      - Defines the method's responses and specifies desired parameter
        mappings or payload mappings from integration responses to method
        responses.

The following example defines an operation that uses a mock integration.

..
    TODO: Add smithy example

.. code-block:: json

    {
        "smithy": "0.4.0",
        "smithy.example": {
            "shapes": {
                "MyOperation": {
                    "type": "operation",
                    "http": {"method": "POST", "uri": "/2"},
                    "aws.apigateway#mockIntegration": {
                        "requestTemplates" : {
                            "application/json" : "#set ($root=$input.path('$')) { \"stage\": \"$root.name\", \"user-id\": \"$root.key\" }",
                            "application/xml" : "#set ($root=$input.path('$')) <stage>$root.name</stage> "
                        },
                        "requestParameters" : {
                            "integration.request.path.stage" : "method.request.querystring.version",
                            "integration.request.querystring.provider" : "method.request.querystring.vendor"
                        },
                        "responses" : {
                            "2\\d{2}" : {
                                "statusCode" : "200",
                                "responseParameters" : {
                                    "method.response.header.requestId" : "integration.response.header.cid"
                                },
                                "responseTemplates" : {
                                    "application/json" : "#set ($root=$input.path('$')) { \"stage\": \"$root.name\", \"user-id\": \"$root.key\" }",
                                    "application/xml" : "#set ($root=$input.path('$')) <stage>$root.name</stage> "
                                }
                            },
                            "302" : {
                                "statusCode" : "302",
                                "responseParameters" : {
                                    "method.response.header.Location" : "integration.response.body.redirect.url"
                                }
                            },
                            "default" : {
                                "statusCode" : "400",
                                "responseParameters" : {
                                    "method.response.header.test-method-response-header" : "'static value'"
                                }
                            }
                        }
                    }
                }
            }
        }
    }

.. note::

    This trait should be considered internal-only and not exposed to your
    customers.


Shared trait data types
=======================

The following shapes are used throughout the Smithy API Gateway traits
definitions.


.. _apigateway-content-handling:

ContentHandling string
----------------------

Defines the payload conversion handling of a request or response.
Valid values are:

- CONVERT_TO_TEXT: for converting a binary payload into a
  Base64-encoded string or converting a text payload into a
  utf-8-encoded string or passing through the text payload natively
  without modification
- CONVERT_TO_BINARY: for converting a text payload into
  Base64-decoded blob or passing through a binary payload natively
  without modification.


.. _apigateway-requestParameters:

requestParameters object
------------------------

Specifies mappings from named method request parameters to integration
request parameters. The method request parameters must be defined before
they are referenced.

**Properties**

.. list-table::
    :header-rows: 1
    :widths: 30 10 60

    * - Property
      - Type
      - Description
    * - ``integration.request.<param-type>.<param-name>``
      - string
      - The value must be a predefined method request parameter of the
        ``method.request.<param-type>.<param-name>`` format, where
        ``<param-type>`` can be querystring, path, header, or body. For
        the body parameter, the ``<param-name>`` is a JSON path expression
        without the ``$.`` prefix.

The following request parameter mappings example translates a method
request's query (version), header (x-user-id) and path (service)
parameters to the integration request's query (stage),
header (x-userid), and path parameters (op), respectively.

.. code-block:: json

    {
        "requestParameters" : {
            "integration.request.querystring.stage" : "method.request.querystring.version",
            "integration.request.header.x-userid" : "method.request.header.x-user-id",
            "integration.request.path.op" : "method.request.path.service"
        }
    }


.. _apigateway-requestTemplates:

requestTemplates object
-----------------------

Specifies mapping templates for a request payload of the specified media types.

**Properties**

.. list-table::
    :header-rows: 1
    :widths: 15 15 70

    * - Property
      - Type
      - Description
    * - ``<Media type>``
      - string
      - A `mapping template <mapping templates>`_.

The following example sets mapping templates for a request payload of the
``application/json`` and ``application/xml`` media types.

.. code-block:: json

    {
        "requestTemplates" : {
            "application/json" : "#set ($root=$input.path('$')) { \"stage\": \"$root.name\", \"user-id\": \"$root.key\" }",
            "application/xml" : "#set ($root=$input.path('$')) <stage>$root.name</stage> "
        }
    }


.. _apigateway-responses:

responses object
----------------

Defines the method's responses and specifies parameter mappings or payload
mappings from integration responses to method responses.

**Properties**

.. list-table::
    :header-rows: 1
    :widths: 20 10 70

    * - Property
      - Type
      - Description
    * - ``<Response status pattern>``
      - :ref:`Response object <apigateway-response-object>`
      - Selection regular expression used to match the integration response
        to the method response. For HTTP integrations, this regex applies to
        the integration response status code. For Lambda invocations, the
        regex applies to the errorMessage field of the error information
        object returned by AWS Lambda as a failure response body when the
        Lambda function execution throws an exception.

        .. note::

            The Response status pattern property name refers to a response
            status code or regular expression describing a group of response
            status codes. It does not correspond to any identifier of an
            `IntegrationResponse`_ resource in the API Gateway REST API.

The following example shows a list of responses from ``2xx`` and ``302``
responses. For the ``2xx`` response, the method response is mapped from
the integration response's payload of the ``application/json`` or
``application/xml`` media type. This response uses the supplied mapping
templates. For the ``302`` response, the method response returns a
``Location`` header whose value is derived from the ``redirect.url``
property on the integration response's payload.

.. code-block:: json

    {
        "responses" : {
            "2\\d{2}" : {
                "statusCode" : "200",
                "responseTemplates" : {
                    "application/json" : "#set ($root=$input.path('$')) { \"stage\": \"$root.name\", \"user-id\": \"$root.key\" }",
                    "application/xml" : "#set ($root=$input.path('$')) <stage>$root.name</stage> "
                }
            },
            "302" : {
                "statusCode" : "302",
                "responseParameters" : {
                    "method.response.header.Location": "integration.response.body.redirect.url"
                }
            }
        }
    }


.. _apigateway-response-object:

response object
---------------

Defines a response and specifies parameter mappings or payload mappings from
the integration response to the method response.

**Properties**

.. list-table::
    :header-rows: 1
    :widths: 30 10 60

    * - Property
      - Type
      - Description
    * - statusCode
      - string
      - HTTP status code for the method response; for example, "200". This
        must correspond to a matching response in the OpenAPI Operation
        responses field.
    * - responseTemplates
      - :ref:`Response templates object <apigateway-response-templates-object>`
      - Specifies media type-specific mapping templates for the response's
        payload.
    * - responseParameters
      - :ref:`Response parameters object <apigateway-response-parameters-object>`
      - Specifies parameter mappings for the response. Only the header and
        body parameters of the integration response can be mapped to the header
        parameters of the method.
    * - contentHandling
      - :ref:`ContentHandling string <apigateway-content-handling>`
      - Response payload content handling.

The following example defines a 302 response for the method that derives a
payload of the ``application/json`` or ``application/xml`` media type from the
backend. The response uses the supplied mapping templates and returns the
redirect URL from the integration response in the method's Location header.

.. code-block:: json

    {
        "statusCode" : "302",
        "responseTemplates" : {
             "application/json" : "#set ($root=$input.path('$')) { \"stage\": \"$root.name\", \"user-id\": \"$root.key\" }",
             "application/xml" : "#set ($root=$input.path('$')) <stage>$root.name</stage> "
        },
        "responseParameters" : {
            "method.response.header.Location": "integration.response.body.redirect.url"
        }
    }


.. _apigateway-response-templates-object:

Response templates object
-------------------------

Specifies mapping templates for a response payload of the specified
media types.

**Properties**

.. list-table::
    :header-rows: 1
    :widths: 30 10 60

    * - Property
      - Type
      - Description
    * - ``<Media type>``
      - string
      - Specifies a mapping template to transform the integration response
        body to the method response body for a given media type. For
        information about creating a mapping template, see
        `mapping Templates`_. An example of a media type is
        ``application/json``.

The following example sets mapping templates for a request payload of the
``application/json`` and ``application/xml`` media types.

.. code-block:: json

    {
        "responseTemplates" : {
            "application/json" : "#set ($root=$input.path('$')) { \"stage\": \"$root.name\", \"user-id\": \"$root.key\" }",
            "application/xml" : "#set ($root=$input.path('$')) <stage>$root.name</stage> "
        }
    }


.. _apigateway-response-parameters-object:

Response parameters object
--------------------------

Specifies mappings from integration method response parameters to method
response parameters. Only the ``header`` and ``body`` types of the integration
response parameters can be mapped to the ``header`` type of the method
response.

**Properties**

.. list-table::
    :header-rows: 1
    :widths: 30 10 60

    * - Property
      - Type
      - Description
    * - ``method.response.header.<param-name>``
      - string
      - The named parameter value can be derived from the header and body
        types of the integration response parameters only.

The following example maps ``body`` and ``header`` parameters of the
integration response to two ``header`` parameters of the method response.

.. code-block:: json

    {
        "responseParameters" : {
            "method.response.header.Location" : "integration.response.body.redirect.url",
            "method.response.header.x-user-id" : "integration.response.header.x-userid"
        }
    }


.. _Enable Request Validation in API Gateway: https://docs.aws.amazon.com/apigateway/latest/developerguide/api-gateway-method-request-validation.html
.. _x-amazon-apigateway-request-validator: https://docs.aws.amazon.com/apigateway/latest/developerguide/api-gateway-swagger-extensions-request-validators.requestValidator.html
.. _x-amazon-apigateway-request-validators: https://docs.aws.amazon.com/apigateway/latest/developerguide/api-gateway-swagger-extensions-request-validators.html
.. _Granting Permissions Using a Resource Policy: https://docs.aws.amazon.com/lambda/latest/dg/intro-permission-model.html#intro-permission-model-access-policy
.. _Integration.passthroughBehavior: https://docs.aws.amazon.com/apigateway/api-reference/resource/integration/#passthroughBehavior
.. _VpcLink: https://docs.aws.amazon.com/apigateway/api-reference/resource/vpc-link/
.. _x-amazon-apigateway-integration: https://docs.aws.amazon.com/apigateway/latest/developerguide/api-gateway-swagger-extensions-integration.html
.. _API Gateway integration: https://docs.aws.amazon.com/apigateway/api-reference/resource/integration/
.. _Lambda authorizers: https://docs.aws.amazon.com/apigateway/latest/developerguide/api-gateway-swagger-extensions-authorizer.html
.. _x-amazon-apigateway-authtype: https://docs.aws.amazon.com/apigateway/latest/developerguide/api-gateway-swagger-extensions-authtype.html
.. _Create and Use Usage Plans with API Keys: https://docs.aws.amazon.com/apigateway/latest/developerguide/api-gateway-api-usage-plans.html
.. _Choose an API Key Source: https://docs.aws.amazon.com/apigateway/latest/developerguide/api-gateway-api-key-source.html
.. _x-amazon-apigateway-api-key-source: https://docs.aws.amazon.com/apigateway/latest/developerguide/api-gateway-swagger-extensions-api-key-source.html
.. _IntegrationResponse: https://docs.aws.amazon.com/apigateway/api-reference/resource/integration-response/
.. _mapping Templates: https://docs.aws.amazon.com/apigateway/latest/developerguide/models-mappings.html#models-mappings-mappings
