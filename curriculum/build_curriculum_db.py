import sqlite3
import json
import os
import sys
import shutil

# Ensure import of quiz_translations works from any working directory
SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
if SCRIPT_DIR not in sys.path:
    sys.path.insert(0, SCRIPT_DIR)
from quiz_translations import QUIZ_TRANSLATIONS

WORKSPACE_DIR = os.path.abspath(os.path.join(SCRIPT_DIR, ".."))
LOCAL_DB_PATH = os.path.join(SCRIPT_DIR, "curriculum_v1.db")
ANDROID_ASSET_DB_PATH = os.path.join(WORKSPACE_DIR, "android/app/src/main/assets/curriculum_v1.db")
LEGACY_ASSET_DB_PATH = os.path.join(WORKSPACE_DIR, "executorch-examples/llm/android/LlamaDemo/app/src/main/assets/curriculum_v1.db")

target_paths = [LOCAL_DB_PATH, ANDROID_ASSET_DB_PATH]
if os.path.exists(os.path.dirname(LEGACY_ASSET_DB_PATH)):
    target_paths.append(LEGACY_ASSET_DB_PATH)

for path in target_paths:
    if os.path.exists(path):
        os.remove(path)

conn = sqlite3.connect(LOCAL_DB_PATH)
cur = conn.cursor()

cur.execute("""
CREATE TABLE skills_catalog (
    skill_id TEXT PRIMARY KEY,
    domain_category TEXT NOT NULL,
    prerequisite_ids TEXT NOT NULL,
    base_hours INTEGER NOT NULL,
    default_p_l0 REAL NOT NULL,
    syllabus_en TEXT NOT NULL,
    diagnostic_quiz_json TEXT NOT NULL
);
""")

cur.execute("""
CREATE TABLE indic_content_fts (
    skill_id TEXT NOT NULL,
    lang_code TEXT NOT NULL,
    localized_title TEXT NOT NULL,
    cultural_analogy TEXT NOT NULL,
    syllabus_localized TEXT NOT NULL,
    quiz_localized_json TEXT NOT NULL DEFAULT '[]',
    PRIMARY KEY (skill_id, lang_code)
);
""")

cur.execute("""
CREATE INDEX idx_indic_skill_lang ON indic_content_fts(skill_id, lang_code);
""")

# 20 skills across 6 domains:
# Domain 1: Technical Roadmaps & Arm SoC Edge AI (5 skills - Flagship Course)
# Domain 2: Exam Preparation (3 skills)
# Domain 3: Certification Paths (3 skills)
# Domain 4: Career Guidance (3 skills)
# Domain 5: Skill Development (3 skills)
# Domain 6: Lifelong Learning Plans (3 skills)

skills_data = [
    # =========================================================================
    # FLAGSHIP COURSE: Edge AI & On-Device LLM Engineering on Arm (5 Tiers)
    # =========================================================================
    (
        "edge_ai_01_tensors",
        "Technical Roadmaps & Arm SoC Edge AI",
        json.dumps([]),
        6,
        0.30,
        "Tensors, Quantization Math & Float16 vs INT4: Scaling factors, zero-point quantization, affine transformations, and dynamic activation range clipping.",
        json.dumps([
            {
                "q": "What is the primary formula for uniform affine asymmetric quantization from floating-point x to integer q?",
                "options": ["q = round(x / S) + Z", "q = x * S - Z", "q = floor(x) / Z", "q = ceil(x * Z + S)"],
                "ans": 0,
                "explanation": "Uniform affine quantization maps real float values to integers using scale factor S and zero-point integer offset Z: q = clamp(round(x/S) + Z, qmin, qmax)."
            },
            {
                "q": "How many bits are saved when compressing a 1-billion parameter model from FP16 (16-bit) to INT4 (4-bit)?",
                "options": ["75% reduction (from ~2.0 GB down to ~0.5-0.75 GB)", "25% reduction", "50% reduction", "90% reduction"],
                "ans": 0,
                "explanation": "FP16 requires 16 bits (2 bytes) per weight. INT4 requires 4 bits (0.5 bytes) per weight, achieving an exact 4x (75%) compression ratio."
            },
            {
                "q": "Why is dynamic activation quantization essential when quantizing LLM weights to INT4 on Arm Cortex processors?",
                "options": ["Activations vary with input prompt tokens and need runtime dynamic scaling to preserve perplexity", "To increase disk storage", "To disable backpropagation", "To convert integer weights to strings"],
                "ans": 0,
                "explanation": "Unlike static weights, activation distributions shift dynamically with each prompt; dynamic quantization computes scaling per token."
            }
        ])
    ),
    (
        "edge_ai_02_spinquant",
        "Technical Roadmaps & Arm SoC Edge AI",
        json.dumps(["edge_ai_01_tensors"]),
        8,
        0.25,
        "SpinQuant Outlier Suppression & Cayley Rotations: Orthogonal matrix transformations, Cayley parametrization, and eliminating activation outlier channels without extra parameters.",
        json.dumps([
            {
                "q": "What core problem in sub-4-bit LLM quantization does SpinQuant solve?",
                "options": ["Severe activation outlier spikes concentrated in specific hidden channels that distort quantization grids", "Excessive GPU fan noise", "Lack of tokenizer vocabulary", "Slow SQLite database reads"],
                "ans": 0,
                "explanation": "LLMs exhibit systematic high-magnitude activation outliers; SpinQuant applies learned orthogonal rotations to spread energy uniformly."
            },
            {
                "q": "How does SpinQuant optimize orthogonal rotation matrices while guaranteeing strict mathematical orthogonality (R^T * R = I)?",
                "options": ["Using Cayley parametrization: R = (I - S)(I + S)^(-1) where S is skew-symmetric", "Using random gradient descent without constraints", "By setting all diagonal elements to zero", "Through singular value clipping"],
                "ans": 0,
                "explanation": "Cayley transform maps unconstrained skew-symmetric matrices (S = -S^T) directly to valid orthogonal rotation matrices R."
            },
            {
                "q": "Does applying SpinQuant orthogonal rotations add runtime parameter or latency overhead to Llama inference?",
                "options": ["Zero runtime overhead because rotations are fused directly into adjacent linear weight matrices during offline export", "Adds 50% extra weights", "Requires dual NPU passes", "Slows down decoding by 10x"],
                "ans": 0,
                "explanation": "Orthogonal matrices R commute with linear projections: W_rotated = W * R, allowing offline fusion into weights prior to PTE export."
            }
        ])
    ),
    (
        "edge_ai_03_kleidiai",
        "Technical Roadmaps & Arm SoC Edge AI",
        json.dumps(["edge_ai_02_spinquant"]),
        10,
        0.20,
        "Arm Neon SIMD & KleidiAI Vector Micro-Kernels: 128-bit vector registers, sdot/udot dot-product arithmetic, i8mm matrix instructions, and Cortex-X/A7xx big.LITTLE core pinning.",
        json.dumps([
            {
                "q": "Which Arm Neon instruction computes a dot product of four 8-bit integers and accumulates into a 32-bit vector lane in a single cycle?",
                "options": ["SDOT (Signed Dot Product)", "FMLA (Floating-point Multiply Accumulate)", "LDR (Load Register)", "SVC (Supervisor Call)"],
                "ans": 0,
                "explanation": "The Armv8.2-A SDOT instruction performs four signed 8-bit integer multiplications and adds them to a 32-bit accumulator lane in one cycle."
            },
            {
                "q": "What is the primary role of Arm KleidiAI micro-kernels integrated into ExecuTorch's XNNPACK backend?",
                "options": ["Hand-optimized assembly kernels tailoring matrix GEMM/GEMV operations directly to Cortex-X and Cortex-A CPU vector pipelines", "A cloud dashboard for analytics", "An Android layout inspector", "A wireless debugger"],
                "ans": 0,
                "explanation": "KleidiAI provides lightweight, assembly-level optimized kernels specifically tuned for Arm Neon and SME to deliver peak token/sec throughput."
            },
            {
                "q": "Why is big.LITTLE thread affinity (pinning ExecuTorch worker threads to Cortex-X or Cortex-A720 cores) advantageous?",
                "options": ["Prevents thread migration to low-power Cortex-A520 efficiency cores that would bottleneck token throughput below 22 t/s", "Saves screen battery", "Disables camera sensors", "Increases flash storage space"],
                "ans": 0,
                "explanation": "Restricting ExecuTorch execution to high-performance cores avoids scheduling jitter and thermal throttling on efficiency cores."
            }
        ])
    ),
    (
        "edge_ai_04_executorch",
        "Technical Roadmaps & Arm SoC Edge AI",
        json.dumps(["edge_ai_03_kleidiai"]),
        10,
        0.20,
        "ExecuTorch Android Runtime & Memory Sandboxing: Ahead-of-time planned memory buffers, PTE flatbuffer serialization, mmap zero-copy weights, and JNI bridge isolation.",
        json.dumps([
            {
                "q": "How does ExecuTorch achieve predictable, non-fragmented memory allocation on resource-constrained Android devices?",
                "options": ["Ahead-of-time memory planning: activation and workspace tensor buffers are calculated and fixed during offline model export", "Dynamic malloc calls on every token generated", "Virtual memory paging over WiFi", "Garbage collection pauses"],
                "ans": 0,
                "explanation": "ExecuTorch precomputes maximum overlapping tensor lifetimes, allocating a single deterministic memory arena."
            },
            {
                "q": "Why should ExecuTorch models on Android prefer mmap loading over copying the entire .pte file into heap memory?",
                "options": ["mmap maps file pages directly into virtual address space on-demand, avoiding doubling anonymous heap memory (libc_malloc)", "mmap encrypts the weights", "mmap increases model accuracy", "mmap converts models to Python"],
                "ans": 0,
                "explanation": "Loading via File read doubles memory (heap buffer + mmap/execution buffers). Direct mmap lets the OS page in weights with zero heap duplication."
            },
            {
                "q": "What happens if an Android app attempts to load two ExecuTorch Llama-1B instances simultaneously in the same process?",
                "options": ["VmRSS doubles from ~1.4 GB to >2.6 GB, instantly violating the strict <1.8 GB benchmark ceiling", "The app doubles its generation speed", "The model switches to 8-bit precision", "CPU temperature drops"],
                "ans": 0,
                "explanation": "Each 1B model instance occupies ~1.1-1.4 GB of resident memory. Concurrently loading two breaches the mobile memory envelope."
            }
        ])
    ),
    (
        "edge_ai_05_kvcache",
        "Technical Roadmaps & Arm SoC Edge AI",
        json.dumps(["edge_ai_04_executorch"]),
        12,
        0.15,
        "Attention Sinks, Ring Buffer KV-Cache & Latency Optimization: StreamingLLM attention sinks, sliding-window KV-cache eviction, and bounding process VmRSS under 1800 MB.",
        json.dumps([
            {
                "q": "Why does standard Key-Value (KV) cache expansion pose a fatal threat to long-running on-device mobile LLM tutoring sessions?",
                "options": ["KV-cache memory grows linearly with sequence length O(N), rapidly triggering mobile Linux OOM (Out-of-Memory) killers", "It increases model weight size", "It disables the Android touch screen", "It causes battery overcharging"],
                "ans": 0,
                "explanation": "Every new conversational turn appends K and V tensors across all layers. Without eviction, RAM increases until Android OS kills the process."
            },
            {
                "q": "What is the core discovery of 'Attention Sinks' in StreamingLLM?",
                "options": ["Initial prompt tokens (first 4 tokens) absorb a disproportionate amount of softmax attention mass and must never be evicted", "All punctuation tokens can be deleted", "Attention heads can be turned off", "Only the last token matters"],
                "ans": 0,
                "explanation": "Softmax requires attention weights to sum to 1. Early tokens act as an attention sink; preserving them maintains stable perplexity indefinitely."
            },
            {
                "q": "How does a sink-token sliding window KV-cache manager enforce the <1.8 GB RAM constraint during extended dialogue?",
                "options": ["Retains 4 sink tokens + 380 sliding window tokens in a fixed ring buffer, bounding KV-cache RAM to a flat ~150 MB ceiling", "Deletes the model from disk", "Transfers conversational state to cloud servers", "Compresses audio files"],
                "ans": 0,
                "explanation": "A fixed-size ring buffer guarantees that KV-cache memory remains constant regardless of whether the student chats for 10 turns or 1,000 turns."
            }
        ])
    ),

    # =========================================================================
    # DOMAIN 2: Exam Preparation (GATE CS & Algorithms)
    # =========================================================================
    (
        "gate_01_dsa",
        "Exam Preparation",
        json.dumps([]),
        8,
        0.35,
        "GATE CS Data Structures: Asymptotic notations, AVL/Red-Black trees, binary heaps, Dijkstra's shortest path, and dynamic programming recurrence relations.",
        json.dumps([
            {
                "q": "What is the tightest worst-case time complexity of searching an element in an AVL tree with n nodes?",
                "options": ["O(log n)", "O(n)", "O(n log n)", "O(1)"],
                "ans": 0,
                "explanation": "AVL trees maintain a strict height balance factor of in {-1, 0, 1}, guaranteeing height h <= 1.44 log2(n), hence O(log n) search."
            },
            {
                "q": "Which data structure is optimal for implementing Dijkstra's algorithm to achieve O((V + E) log V) time?",
                "options": ["Min-Heap (Priority Queue)", "Stack", "Queue", "Adjacency Matrix"],
                "ans": 0,
                "explanation": "A binary min-heap allows extracting minimum distance vertex in O(log V) and decreasing keys in O(log V)."
            },
            {
                "q": "In the 0/1 Knapsack dynamic programming problem, what does the state DP[i][w] represent?",
                "options": ["Maximum value obtainable using a subset of first i items within weight capacity w", "Minimum weight of first i items", "Total value of all items combined", "Number of ways to fill knapsack"],
                "ans": 0,
                "explanation": "DP[i][w] = max(DP[i-1][w], DP[i-1][w-wt[i-1]] + val[i-1])."
            }
        ])
    ),
    (
        "gate_02_os",
        "Exam Preparation",
        json.dumps(["gate_01_dsa"]),
        10,
        0.25,
        "Operating Systems: Process synchronization, Semaphores, Peterson's algorithm, Banker's algorithm for deadlock avoidance, and virtual memory paging.",
        json.dumps([
            {
                "q": "Which condition is NOT one of the four Coffman conditions necessary for a deadlock to occur?",
                "options": ["Preemption allowed by kernel", "Mutual Exclusion", "Hold and Wait", "Circular Wait"],
                "ans": 0,
                "explanation": "Deadlock requires No Preemption. If resources can be preempted, deadlock cannot form."
            },
            {
                "q": "In virtual memory systems, what is Belady's Anomaly?",
                "options": ["Increasing the number of page frames causes more page faults under FIFO replacement", "LRU page replacement failing on small caches", "Thrashing due to excessive CPU utilization", "TLB misses exceeding cache size"],
                "ans": 0,
                "explanation": "Belady's Anomaly demonstrates that for certain page reference strings, FIFO page replacement produces more faults with more frames."
            },
            {
                "q": "What does Banker's algorithm verify before allocating requested resources to a process?",
                "options": ["Whether the resulting state remains in a Safe State where at least one execution sequence exists", "Whether CPU load is below 50%", "Whether network latency is low", "Whether process priority is highest"],
                "ans": 0,
                "explanation": "Banker's algorithm checks if there is a safe sequence <P1, P2, ... Pn> such that each process's maximum need can be satisfied."
            }
        ])
    ),
    (
        "gate_03_dbms",
        "Exam Preparation",
        json.dumps(["gate_02_os"]),
        8,
        0.30,
        "DBMS: Relational algebra, BCNF/3NF normalization, ACID transaction properties, serializability, and Two-Phase Locking (2PL) protocols.",
        json.dumps([
            {
                "q": "What condition must hold for a functional dependency X -> Y in a relation R to satisfy Boyce-Codd Normal Form (BCNF)?",
                "options": ["X must be a superkey of relation R (or Y is a subset of X)", "Y must be a prime attribute", "X must be a candidate key and Y non-prime", "R must have fewer than 3 attributes"],
                "ans": 0,
                "explanation": "BCNF requires that for every non-trivial functional dependency X -> Y, the determinant X is a superkey."
            },
            {
                "q": "What does the 'I' (Isolation) in ACID guarantee during concurrent database transaction execution?",
                "options": ["Concurrent transactions execute without interfering with one another, appearing as if executed serially", "Data is immediately replicated to secondary disks", "Transactions execute within 1 millisecond", "Transactions never abort"],
                "ans": 0,
                "explanation": "Isolation prevents phenomena like dirty reads, non-repeatable reads, and phantom reads across concurrent transactions."
            },
            {
                "q": "Under the Strict Two-Phase Locking (Strict 2PL) protocol, when are exclusive write locks released?",
                "options": ["Only after the transaction commits or aborts", "Immediately after the write statement executes", "During the growing phase", "Whenever another transaction requests them"],
                "ans": 0,
                "explanation": "Strict 2PL holds all exclusive locks until transaction completion (commit/abort), preventing cascading aborts."
            }
        ])
    ),

    # =========================================================================
    # DOMAIN 3: Certification Paths (Cloud Architecture & DevSecOps)
    # =========================================================================
    (
        "cert_01_cloud_sec",
        "Certification Paths",
        json.dumps([]),
        8,
        0.30,
        "Cloud Security & Zero Trust Architecture: Identity and Access Management (IAM), principle of least privilege, mutual TLS, and AWS/Azure KMS encryption.",
        json.dumps([
            {
                "q": "What is the foundational premise of Zero Trust Network Architecture (ZTNA)?",
                "options": ["Never trust, always verify: authenticate and authorize every request regardless of origin", "Trust all connections originating inside the local corporate subnet", "Disable HTTPS encryption on internal networks", "Rely entirely on static perimeter firewalls"],
                "ans": 0,
                "explanation": "Zero Trust assumes hostile actors can be inside the perimeter; every session must be authenticated and encrypted."
            },
            {
                "q": "What does the Principle of Least Privilege (PoLP) dictate in cloud IAM role policies?",
                "options": ["Grant only the minimum permissions necessary for an identity to complete its intended task", "Grant AdministratorAccess to avoid permission tickets", "Share root user credentials across teams", "Allow wildcard '*' actions on all S3 buckets"],
                "ans": 0,
                "explanation": "Least privilege limits blast radius by ensuring principals only hold access required for specific operational duties."
            },
            {
                "q": "What is the primary advantage of Envelope Encryption using AWS KMS customer master keys (KMS CMK)?",
                "options": ["Protects plaintext data with a unique Data Encryption Key (DEK), encrypting only the DEK under the root KMS key", "Transfers data without encryption over public internet", "Stores plaintext passwords in Git", "Eliminates need for TLS certificates"],
                "ans": 0,
                "explanation": "Envelope encryption allows fast local symmetric data encryption via DEKs without sending bulk data over the KMS network API."
            }
        ])
    ),
    (
        "cert_02_k8s_containers",
        "Certification Paths",
        json.dumps(["cert_01_cloud_sec"]),
        10,
        0.20,
        "Kubernetes & Container Orchestration: Pod lifecycles, ConfigMaps/Secrets, Service types (ClusterIP, NodePort, LoadBalancer), and horizontal pod autoscaling.",
        json.dumps([
            {
                "q": "What is the smallest deployable computing unit in Kubernetes?",
                "options": ["Pod", "Container", "Node", "Namespace"],
                "ans": 0,
                "explanation": "A Pod encapsulates one or more containers sharing network namespaces and storage volumes."
            },
            {
                "q": "Which Kubernetes Service type exposes a set of pods internally within the cluster using a virtual IP?",
                "options": ["ClusterIP", "NodePort", "LoadBalancer", "ExternalName"],
                "ans": 0,
                "explanation": "ClusterIP is the default service type, providing intra-cluster routing accessible only within the cluster network."
            },
            {
                "q": "What metric does the Kubernetes Horizontal Pod Autoscaler (HPA) evaluate by default to scale replicas?",
                "options": ["Observed CPU/Memory utilization against targeted threshold percentages", "Number of lines in pod log files", "Total disk space of the master node", "WiFi signal strength"],
                "ans": 0,
                "explanation": "HPA queries the metrics-server to compare current container CPU or memory usage against deployment target thresholds."
            }
        ])
    ),
    (
        "cert_03_devops_ci",
        "Certification Paths",
        json.dumps(["cert_02_k8s_containers"]),
        12,
        0.15,
        "DevOps CI/CD & Infrastructure as Code: GitHub Actions workflows, Terraform state management, immutable infrastructure, and Canary deployment strategies.",
        json.dumps([
            {
                "q": "Why is storing Terraform state file in a remote backend with distributed state locking (e.g., S3 + DynamoDB) mandatory in team environments?",
                "options": ["Prevents concurrent terraform apply executions from corrupting shared cloud resource state", "Reduces AWS bill by 50%", "Compiles Terraform to C++", "Eliminates Docker images"],
                "ans": 0,
                "explanation": "Distributed locking ensures that two engineers running terraform apply simultaneously do not generate conflicting resource updates."
            },
            {
                "q": "What characterizes a Canary deployment rollout strategy in production systems?",
                "options": ["Directing a small percentage of production traffic to the new version before promoting it to 100% of users", "Shutting down the entire system for 4 hours of maintenance", "Replacing all instances simultaneously without testing", "Deploying code only on staging servers"],
                "ans": 0,
                "explanation": "Canary releases allow real-world telemetry monitoring on a fraction of user traffic, enabling instant rollback on anomalies."
            },
            {
                "q": "What is the core tenet of 'Immutable Infrastructure'?",
                "options": ["Servers are never patched or modified in-place; new updated container/VM images are launched to replace old instances", "Servers run without rebooting for 10 years", "Root passwords can never be changed", "Firewall rules are permanent"],
                "ans": 0,
                "explanation": "Immutable infrastructure treats servers as disposable cattle, preventing configuration drift across server fleets."
            }
        ])
    ),

    # =========================================================================
    # DOMAIN 4: Career Guidance (Data Engineering & Tech Transitions)
    # =========================================================================
    (
        "career_01_data_analyst",
        "Career Guidance",
        json.dumps([]),
        8,
        0.35,
        "SQL Analytics & Business Intelligence: Complex SQL joins, window functions (ROW_NUMBER, RANK, LAG/LEAD), CTEs, and cohort retention modeling.",
        json.dumps([
            {
                "q": "What is the difference between RANK() and DENSE_RANK() window functions when handling tied values?",
                "options": ["RANK() skips subsequent rank numbers after ties; DENSE_RANK() does not leave gaps", "DENSE_RANK() skips ranks; RANK() leaves no gaps", "RANK() works only on integers; DENSE_RANK() on strings", "Both functions behave identically"],
                "ans": 0,
                "explanation": "With values (100, 100, 90), RANK yields (1, 1, 3); DENSE_RANK yields (1, 1, 2)."
            },
            {
                "q": "Which SQL window function retrieves data from an immediately preceding row without requiring a self-join?",
                "options": ["LAG()", "LEAD()", "FIRST_VALUE()", "ROW_NUMBER()"],
                "ans": 0,
                "explanation": "LAG(column, offset) accesses a prior row at a given physical offset within the current window partition."
            },
            {
                "q": "Why are Common Table Expressions (CTEs, WITH clause) preferred over deep nested subqueries in production analytics?",
                "options": ["Significantly enhance query readability, modularity, and enable recursive traversal", "They eliminate the need for SQL indexes", "They bypass query optimizer validation", "They convert SQL queries into Python"],
                "ans": 0,
                "explanation": "CTEs structure complex transformation stages into readable, named sequential blocks."
            }
        ])
    ),
    (
        "career_02_data_pipeline",
        "Career Guidance",
        json.dumps(["career_01_data_analyst"]),
        10,
        0.25,
        "Data Warehousing & Orchestration: Star schema modeling, slowly changing dimensions (SCD Type 2), Apache Airflow DAG scheduling, and batch vs streaming ETL.",
        json.dumps([
            {
                "q": "In Kimball dimensional modeling, what is the key design characteristic of a Star Schema?",
                "options": ["A centralized Fact table containing numerical metrics connected directly to denormalized Dimension tables", "A normalized database with no duplicate foreign keys", "A single flat file without relations", "An interconnected graph with cycles"],
                "ans": 0,
                "explanation": "Star schema centers around a central fact table surrounded by denormalized dimension tables for fast analytical querying."
            },
            {
                "q": "How does a Slowly Changing Dimension Type 2 (SCD Type 2) track historical changes in dimension attributes?",
                "options": ["Creates a new record with effective start/end timestamps and an active flag, preserving full audit history", "Overwrites existing record in-place", "Appends a new column for every edit", "Deletes all historical transactions"],
                "ans": 0,
                "explanation": "SCD2 maintains historical fidelity by versioning dimensional rows with validity intervals."
            },
            {
                "q": "What is the primary architectural role of Apache Airflow in enterprise data platforms?",
                "options": ["Programmatic DAG orchestration: scheduling, executing, and monitoring complex data dependency workflows", "In-memory database engine", "Frontend dashboard builder", "Git repository host"],
                "ans": 0,
                "explanation": "Airflow defines workflows as Directed Acyclic Graphs (DAGs) in Python, managing task dependencies and retries."
            }
        ])
    ),
    (
        "career_03_interview_prep",
        "Career Guidance",
        json.dumps(["career_02_data_pipeline"]),
        12,
        0.20,
        "Senior Tech Interview Case Studies: High-level system design, CAP theorem trade-offs, consistent hashing, rate limiters, and behavioral STAR frameworks.",
        json.dumps([
            {
                "q": "Under Eric Brewer's CAP Theorem, what fundamental trade-off must a distributed system make during a network partition (P)?",
                "options": ["Choose between Consistency (C) and Availability (A)", "Choose between Cost and Latency", "Choose between Memory and Disk", "Choose between Python and Java"],
                "ans": 0,
                "explanation": "When network partitions occur, a distributed database must either reject updates (favoring C) or accept stale writes (favoring A)."
            },
            {
                "q": "Why is Consistent Hashing critical in distributed caching layers (e.g., Memcached cluster scaling)?",
                "options": ["Minimizes cache key remapping to K/n keys when a server node is added or removed", "Guarantees 100% cache hit rate", "Encrypts all cache keys with SHA-256", "Sorts keys alphabetically on disk"],
                "ans": 0,
                "explanation": "Consistent hashing arranges nodes on a virtual ring; adding/removing a server only remaps adjacent key ranges."
            },
            {
                "q": "Which algorithm is standard for implementing sliding-window rate limiters across API gateways?",
                "options": ["Token Bucket or Leaky Bucket", "Dijkstra algorithm", "Bubble sort", "Breadth-first search"],
                "ans": 0,
                "explanation": "Token bucket allows bursting while enforcing long-term average request rates predictably."
            }
        ])
    ),

    # =========================================================================
    # DOMAIN 5: Skill Development (Applied AI & Python)
    # =========================================================================
    (
        "skill_01_python_ds",
        "Skill Development",
        json.dumps([]),
        6,
        0.35,
        "Python for Data Science: NumPy vectorized operations, Pandas DataFrame manipulation, memory management, and Matplotlib data visualization.",
        json.dumps([
            {
                "q": "Why are NumPy array vectorized operations orders of magnitude faster than standard Python for loops?",
                "options": ["NumPy executes contiguous memory operations in pre-compiled C loops with SIMD vectorization", "Python loops allocate more disk space", "NumPy runs in the cloud", "NumPy skips arithmetic operations"],
                "ans": 0,
                "explanation": "NumPy arrays have homogeneous data types in contiguous memory, avoiding Python bytecode interpretation overhead."
            },
            {
                "q": "What is the difference between df.loc[] and df.iloc[] indexers in Pandas?",
                "options": ["loc[] is label-based indexing; iloc[] is integer position-based indexing", "iloc[] is label-based; loc[] is integer-based", "loc[] only reads columns; iloc[] only reads rows", "They are identical aliases"],
                "ans": 0,
                "explanation": "df.loc accesses data via row/column labels; df.iloc accesses via zero-based integer index positions."
            },
            {
                "q": "How can you prevent a MemoryError when reading a massive 20 GB CSV file with Pandas on a laptop with 8 GB RAM?",
                "options": ["Use pd.read_csv() with the chunksize parameter to process records in streaming batches", "Rename the CSV file to .txt", "Delete the headers from the file", "Open the file in Microsoft Excel"],
                "ans": 0,
                "explanation": "The chunksize argument returns an iterator over subsets of rows, keeping resident memory usage strictly bounded."
            }
        ])
    ),
    (
        "skill_02_ml_foundations",
        "Skill Development",
        json.dumps(["skill_01_python_ds"]),
        8,
        0.25,
        "Machine Learning Foundations: Linear & Logistic Regression, decision trees, bias-variance trade-off, cross-validation, and ROC-AUC evaluation metrics.",
        json.dumps([
            {
                "q": "What symptom signals high variance (overfitting) in a machine learning model?",
                "options": ["Near-zero training error but high validation/test error", "High error on both training and test datasets", "Model training completes in 1 second", "Model weights are identical"],
                "ans": 0,
                "explanation": "High variance means the model has memorized training noise, failing to generalize to unseen test data."
            },
            {
                "q": "Why is the ROC-AUC score preferred over simple accuracy for evaluating binary classifiers on imbalanced datasets?",
                "options": ["ROC-AUC evaluates sensitivity across all classification thresholds, unaffected by extreme class imbalances", "It only measures positive predictions", "It requires fewer computing resources", "Accuracy is non-numeric"],
                "ans": 0,
                "explanation": "If 99% of samples are negative, a naive model predicting all negatives scores 99% accuracy; ROC-AUC reveals true discrimination ability."
            },
            {
                "q": "What is the primary role of L2 Regularization (Ridge) in linear regression loss functions?",
                "options": ["Penalizes large weight magnitudes (adds lambda * sum(w_i^2) penalty) to shrink coefficients and prevent overfitting", "Forces weights to become exactly zero for feature selection", "Increases training error to 100%", "Converts regression to clustering"],
                "ans": 0,
                "explanation": "L2 Ridge penalty pulls weights smoothly toward zero, mitigating collinearity and extreme weight oscillations."
            }
        ])
    ),
    (
        "skill_03_deep_learning",
        "Skill Development",
        json.dumps(["skill_02_ml_foundations"]),
        10,
        0.20,
        "Deep Learning Fundamentals: Multi-layer perceptrons, convolutional feature maps (CNNs), activation functions (ReLU, GELU), and backprop.",
        json.dumps([
            {
                "q": "Why is the ReLU activation function (max(0, x)) preferred over Sigmoid in deep hidden layers?",
                "options": ["Mitigates the vanishing gradient problem for positive inputs and is computationally cheap", "It outputs bounded probabilities between 0 and 1", "It requires no GPU memory", "It eliminates weights"],
                "ans": 0,
                "explanation": "Sigmoid saturates at both ends causing near-zero gradients; ReLU maintains a constant gradient of 1 for x > 0."
            },
            {
                "q": "What is the primary role of a Dropout layer during neural network training?",
                "options": ["Regularization: randomly deactivates neurons during forward passes to prevent co-adaptation", "Accelerates inference on mobile devices", "Initializes weights to zero", "Increases image resolution"],
                "ans": 0,
                "explanation": "Dropout prevents units from co-adapting, acting as an implicit ensemble technique."
            },
            {
                "q": "What mathematical operation does a 2D convolution perform on an image tensor?",
                "options": ["Sliding element-wise kernel multiplication and summation to extract spatial feature maps", "Matrix inversion", "Fast Fourier transform of audio", "Random noise addition"],
                "ans": 0,
                "explanation": "Convolution slides a small kernel across the input to detect edges, textures, and hierarchical visual patterns."
            }
        ])
    ),

    # =========================================================================
    # DOMAIN 6: Lifelong Learning Plans (Cognitive Habits & Micro-Learning)
    # =========================================================================
    (
        "life_01_micro_habit",
        "Lifelong Learning Plans",
        json.dumps([]),
        4,
        0.40,
        "Cognitive Habit Building: Spaced repetition retention loops, active recall flashcards, and daily 20-minute learning sprints.",
        json.dumps([
            {
                "q": "What is the primary cognitive benefit of spaced repetition over massed practice (cramming)?",
                "options": ["Flattens the Ebbinghaus forgetting curve by reviewing items just before they are forgotten", "Eliminates the need for restful sleep", "Guarantees instant photographic memory", "Reduces the total number of subjects you can learn"],
                "ans": 0,
                "explanation": "Spaced reviews strengthen synaptic plasticity and long-term memory consolidation right at the brink of decay."
            },
            {
                "q": "Why is Active Recall significantly more effective than passive re-reading of textbooks?",
                "options": ["Effortful memory retrieval strengthens neural pathways and pinpoints precise gaps", "It takes zero cognitive effort", "It requires no notes", "It only works for multiple-choice questions"],
                "ans": 0,
                "explanation": "Retrieval practice forces the brain to reconstruct knowledge, creating durable retrieval routes."
            },
            {
                "q": "What is the core idea of James Clear's 'Habit Stacking' for daily study routines?",
                "options": ["Anchoring a new study habit immediately after an established daily routine (e.g. after morning chai)", "Studying for 12 hours straight once a month", "Multitasking while watching movies", "Changing study times randomly every day"],
                "ans": 0,
                "explanation": "Habit stacking pairs a desired behavior with an existing cue in the brain's habit loop."
            }
        ])
    ),
    (
        "life_02_feynman_technique",
        "Lifelong Learning Plans",
        json.dumps(["life_01_micro_habit"]),
        6,
        0.30,
        "The Feynman Technique: Concept deconstruction, ELI5 plain language explanation, identifying knowledge blindspots, and creating cross-domain analogies.",
        json.dumps([
            {
                "q": "What is the crucial second step of the Feynman Technique after picking a concept to study?",
                "options": ["Explain the concept in simple, plain language as if teaching a 10-year-old child without jargon", "Memorize the textbook definition verbatim", "Write a 50-page formal research paper", "Purchase expensive educational software"],
                "ans": 0,
                "explanation": "Attempting to explain a concept simply without academic jargon instantly exposes hidden gaps in true understanding."
            },
            {
                "q": "What does encountering confusion or needing jargon when explaining a concept indicate under the Feynman method?",
                "options": ["A pinpointed knowledge blindspot that must be revisited in primary source material", "That the student is incapable of learning", "That the textbook is wrong", "That the concept has no real-world use"],
                "ans": 0,
                "explanation": "Stumbling during simplified explanation isolates the exact prerequisite concept needing reinforcement."
            },
            {
                "q": "Why do cross-domain cultural analogies (e.g., comparing database locks to Indian temple tokens) improve concept retention?",
                "options": ["They anchor novel abstract concepts to familiar, deeply encoded mental models in the brain", "They replace the need for mathematical rigor", "They reduce vocabulary size", "They bypass memory entirely"],
                "ans": 0,
                "explanation": "Analogies bridge unfamiliar abstractions to well-established schema in cognitive semantic networks."
            }
        ])
    ),
    (
        "life_03_deep_focus",
        "Lifelong Learning Plans",
        json.dumps(["life_02_feynman_technique"]),
        6,
        0.30,
        "Deep Focus & Attention Hygiene: Cal Newport's Deep Work principles, Pomodoro interval cadence, dopamine fasting, and eliminating attention residue.",
        json.dumps([
            {
                "q": "What does Sophie Leroy's concept of 'Attention Residue' demonstrate regarding frequent multitasking?",
                "options": ["A fraction of cognitive attention remains stuck on the prior task, significantly degrading performance on the new task", "Multitasking permanently doubles cognitive capacity", "Humans can consciously process four complex tasks in parallel without latency", "Switching tasks cleanses short-term memory"],
                "ans": 0,
                "explanation": "When shifting from Task A to Task B, your attention does not immediately follow; a residue remains stuck thinking about Task A."
            },
            {
                "q": "Why is the standard 25-minute Pomodoro focus interval followed by a 5-minute break so effective?",
                "options": ["Short, intense sprints coupled with detachment breaks prevent cognitive fatigue while preserving focus", "It aligns with solar cycles", "It allows watching full movies during breaks", "It eliminates the need for lunch"],
                "ans": 0,
                "explanation": "Structured intervals maintain peak prefrontal cortex arousal while scheduled breaks facilitate diffuse-mode background processing."
            },
            {
                "q": "Which environmental cue most effectively triggers deep work immersion?",
                "options": ["A dedicated, clutter-free physical workspace with digital notifications silenced", "A loud television playing in the background", "A smartphone placed directly on top of the keyboard with vibrating alerts", "Constantly switching study locations every 10 minutes"],
                "ans": 0,
                "explanation": "Environmental friction against distraction removes willpower depletion, easing entry into flow states."
            }
        ])
    )
]

# Deterministically rotate correct answer position across indices 0, 1, 2, 3
# and construct localized quiz representations across all 6 languages
skill_quiz_json = {}
skill_localized_quiz = {}

for skill_idx, row in enumerate(skills_data):
    skill_id = row[0]
    raw_questions = QUIZ_TRANSLATIONS[skill_id]
    processed_q_list = []
    localized_pools = {lang: [] for lang in ["en", "hi", "ta", "te", "bn", "mr"]}

    for q_idx, q_item in enumerate(raw_questions):
        target_idx = (skill_idx * 3 + q_idx) % 4
        rotated_opts_i18n = {}
        for lang in ["en", "hi", "ta", "te", "bn", "mr"]:
            opts = list(q_item["options"][lang])
            if target_idx != 0:
                opts[0], opts[target_idx] = opts[target_idx], opts[0]
            rotated_opts_i18n[lang] = opts
            localized_pools[lang].append({
                "q": q_item["q"][lang],
                "options": opts,
                "ans": target_idx,
                "explanation": q_item["explanation"][lang]
            })

        processed_q_list.append({
            "q": q_item["q"]["en"],
            "options": rotated_opts_i18n["en"],
            "ans": target_idx,
            "explanation": q_item["explanation"]["en"],
            "q_i18n": q_item["q"],
            "opts_i18n": rotated_opts_i18n,
            "explanation_i18n": q_item["explanation"]
        })

    skill_quiz_json[skill_id] = json.dumps(processed_q_list, ensure_ascii=False)
    for lang in ["en", "hi", "ta", "te", "bn", "mr"]:
        skill_localized_quiz[(skill_id, lang)] = json.dumps(localized_pools[lang], ensure_ascii=False)

processed_skills_data = []
for skill_idx, row in enumerate(skills_data):
    skill_id, domain_category, prerequisite_ids, base_hours, default_p_l0, syllabus_en, _ = row
    processed_skills_data.append((
        skill_id,
        domain_category,
        prerequisite_ids,
        base_hours,
        default_p_l0,
        syllabus_en,
        skill_quiz_json[skill_id]
    ))

cur.executemany("INSERT INTO skills_catalog VALUES (?, ?, ?, ?, ?, ?, ?);", processed_skills_data)

# Multilingual localized content (English, Hindi, Tamil, Telugu, Bengali, Marathi) for ALL 20 skills!
indic_records = []

def add_indic(skill_id, en_title, en_analogy, en_syl,
              hi_title, hi_analogy, hi_syl,
              ta_title, ta_analogy, ta_syl,
              te_title, te_analogy, te_syl,
              bn_title, bn_analogy, bn_syl,
              mr_title, mr_analogy, mr_syl):
    indic_records.append((skill_id, "en", en_title, en_analogy, en_syl, skill_localized_quiz[(skill_id, "en")]))
    indic_records.append((skill_id, "hi", hi_title, hi_analogy, hi_syl, skill_localized_quiz[(skill_id, "hi")]))
    indic_records.append((skill_id, "ta", ta_title, ta_analogy, ta_syl, skill_localized_quiz[(skill_id, "ta")]))
    indic_records.append((skill_id, "te", te_title, te_analogy, te_syl, skill_localized_quiz[(skill_id, "te")]))
    indic_records.append((skill_id, "bn", bn_title, bn_analogy, bn_syl, skill_localized_quiz[(skill_id, "bn")]))
    indic_records.append((skill_id, "mr", mr_title, mr_analogy, mr_syl, skill_localized_quiz[(skill_id, "mr")]))

# -----------------------------------------------------------------------------
# FLAGSHIP COURSE: Edge AI & Arm Systems (5 Tiers)
# -----------------------------------------------------------------------------

# 1. edge_ai_01_tensors
add_indic(
    "edge_ai_01_tensors",
    "Tensors, Quantization Math & Float16 vs INT4",
    "Think of INT4 quantization like Mumbai Dabbawalas fitting hot tiffins compactly into a calibrated crate without spilling a single drop.",
    "Week 1: Quantization scale and zero-point math. Week 2: Dynamic activation quantization. Week 3: Quantization error and perplexity checks.",
    "टेंसर, क्वांटाइजेशन गणित और Float16 बनाम INT4 (Tensors & INT4)",
    "INT4 क्वांटाइजेशन को मुंबई के डिब्बेवालों की तरह समझें—जो सीमित लकड़ी के खांचों में सैकड़ों टिफिन बिना खाना गिराए एकदम सही वजन में बैठा देते हैं।",
    "सप्ताह 1: क्वांटाइजेशन स्केल S और जीरो-पॉइंट Z का गणित। सप्ताह 2: डायनामिक एक्टिवेशन स्केलिंग। सप्ताह 3: पर्प्लेक्सिटी और एक्यूरेसी सत्यापन।",
    "டென்சர்கள் மற்றும் INT4 குவாண்டைசேஷன் கணிதம் (Tensors & INT4)",
    "INT4 குவாண்டைசேஷன் என்பது மும்பை டப்பாவாலாக்கள் குறைந்த இடத்தில் துல்லியமாக உணவுப் பெட்டிகளை அடுக்கி வைப்பது போன்றது.",
    "வாரம் 1: அளவுகோல் (Scale) மற்றும் ஜீரோ-பாயிண்ட் கணிதம். வாரம் 2: டைனமிக் ஆக்டிவேஷன். வாரம் 3: துல்லியம் மற்றும் இழப்பு சரிபார்ப்பு.",
    "టెన్సర్లు & INT4 క్వాంటైజేషన్ గణితం (Tensors & INT4)",
    "INT4 క్వాంటైజేషన్‌ను ముంబై డబ్బావాలాలు తక్కువ స్థలంలో భోజన డబ్బాలను నష్టం లేకుండా సర్దే నైపుణ్యంతో పోల్చవచ్చు.",
    "వారం 1: స్కేల్ & జీరో-పాయింట్ సూత్రాలు. వారం 2: డైనమిక్ యాక్టివేషన్ స్కేలింగ్. వారం 3: పెర్‌ప్లెక్సిటీ మరియు ఖచ్చితత్వం.",
    "টেনসর এবং INT4 কোয়ান্টাইজেশন গণিত (Tensors & INT4)",
    "INT4 কোয়ান্টাইজেশনকে মুম্বাইয়ের ডাব্বাওয়ালাদের সীমিত জায়গায় শত শত টিফিন বাক্স নিখুঁতভাবে সাজিয়ে রাখার দক্ষতার সাথে তুলনা করা যায়।",
    "সপ্তাহ ১: কোয়ান্টাইজেশন স্কেল ও জিরো-পয়েন্ট গণিত। সপ্তাহ ২: ডায়নামিক অ্যাক্টিভেশন স্কেলিং। সপ্তাহ ৩: পারপ্লেক্সিটি ও মডেলের নির্ভুলতা।",
    "टेन्सर्स आणि INT4 क्वांटायझेशन गणित (Tensors & INT4)",
    "INT4 क्वांटायझेशन म्हणजे मुंबईच्या डबेवाल्यांनी कमी जागेत व्यवस्थित डबे रचण्यासारखे आहे, जेणेकरून जागाही वाचते आणि कामही जलद होते.",
    "आठवडा १: स्केल आणि झिरो-पॉइंट सूत्रे. आठवडा २: डायनॅमिक ॲक्टिव्हेशन स्केलिंग. आठवडा ३: मॉडेल पर्प्लेक्सिटी व अचूकता पडताळणी."
)

# 2. edge_ai_02_spinquant
add_indic(
    "edge_ai_02_spinquant",
    "SpinQuant Outlier Suppression & Cayley Rotations",
    "Think of SpinQuant Cayley rotations like a potter balancing a clay vessel on a spinning wheel—rotating mass dynamically to cancel sudden wobbles without adding heavy mud.",
    "Week 1: Activation outlier spikes in LLM hidden channels. Week 2: Cayley transform and skew-symmetric orthogonal matrices. Week 3: Offline weight fusion.",
    "स्पिनक्वांट आउटलायर सप्रेशन और केली रोटेशन (SpinQuant)",
    "स्पिनक्वांट केली रोटेशन को कुम्हार के चाक पर घूमते मटके के संतुलन की तरह समझें—जहाँ घूर्णन द्वारा असंतुलित उभारों को बिना अतिरिक्त मिट्टी जोड़े समतल कर दिया जाता है।",
    "सप्ताह 1: न्यूरल नेटवर्क में एक्टिवेशन आउटलायर स्पाइक्स। सप्ताह 2: केली ट्रांसफॉर्म R = (I-S)(I+S)^(-1)। सप्ताह 3: ऑफलाइन वेट फ्यूजन।",
    "ஸ்பின்குவாண்ட் மற்றும் கெய்லி சுழற்சிகள் (SpinQuant Rotations)",
    "சிலம்பாட்டத்தில் சுழலும் கம்பு எப்படி எதிர்ப்பை சமமாகப் பகிர்ந்துகொள்கிறதோ, அதுபோல ஸ்பின்குவாண்ட் பெரிய எண்களைச் சுழற்றி சமன் செய்கிறது.",
    "வாரம் 1: அவுட்லையர் ஸ்பைக்குகள். வாரம் 2: கெய்லி உருமாற்றம் மற்றும் செங்குத்து அணிகள். வாரம் 3: எடைகளை இணைத்தல் (Weight Fusion).",
    "స్పిన్‌క్వాంట్ అవుట్‌లయర్ అణచివేత & కేలీ రొటేషన్లు (SpinQuant)",
    "కూచిపూడి భ్రమరి నృత్యంలో గిర్రున తిరుగుతూ సమతుల్యతను కాపాడుకున్నట్లే, స్పిన్‌క్వాంట్ పెద్ద సంఖ్యలను సమానంగా పంపిణీ చేస్తుంది.",
    "వారం 1: యాక్టివేషన్ అవుట్‌లయర్స్. వారం 2: కేలీ ట్రాన్స్‌ఫార్మ్ & ఆర్థోగోనల్ మ్యాట్రిక్స్. వారం 3: ఆఫ్‌లైన్ వెయిట్ ఫ్యూజన్.",
    "স্পিনকোয়ান্ট আউটলায়ার দমন এবং কেলি রোটেশন (SpinQuant)",
    "কুমোরের চাকার ঘূর্ণনের মতো, যা অতিরিক্ত মাটি ছাড়াই অসম খাঁজগুলোকে মসৃণ করে দেয়, স্পিনকোয়ান্ট বড় আউটলায়ারগুলোকে ঘুরিয়ে সমান করে।",
    "সপ্তাহ ১: অ্যাক্টিভেশন আউটলায়ার স্পাইক। সপ্তাহ ২: কেলি রূপান্তর ও অর্থোগোনাল ম্যাট্রিক্স। সপ্তাহ ৩: অফলাইন ওয়েট ফিউশন।",
    "स्पिनक्वांट आउटलायर नियंत्रण आणि केली रोटेशन्स (SpinQuant)",
    "कुंभाराच्या चाकावर फिरणाऱ्या मडक्यासारखे हे आहे—चाक फिरवून जसे खाचखळगे सपाट केले जातात, तसेच स्पिनक्वांट आकड्यांना समतोल करते.",
    "आठवडा १: लपलेल्या चॅनेलचे आउटलायर्स. आठवडा २: केली ट्रान्सफॉर्म आणि ऑर्थोगोनल मॅट्रिक्स. आठवडा ३: ऑफलाइन वेट फ्यूजन."
)

# 3. edge_ai_03_kleidiai
add_indic(
    "edge_ai_03_kleidiai",
    "Arm Neon SIMD & KleidiAI Vector Micro-Kernels",
    "Think of Arm Neon SIMD like a 4-track Indian Railways trunk line where 4 express trains advance concurrently under a single synchronized green signal.",
    "Week 1: 128-bit vector registers and SDOT instructions. Week 2: KleidiAI micro-kernel assembly dispatch. Week 3: Big.LITTLE core pinning and thermal management.",
    "आर्म निऑन सिमड और क्लेडीआई वेक्टर कर्नेल (Arm Neon & KleidiAI)",
    "आर्म निऑन सिमड को भारतीय रेल की चार समानांतर पटरियों की तरह समझें—जहाँ एक ही हरी झंडी मिलते ही चार ट्रेनें एक साथ आगे बढ़ती हैं।",
    "सप्ताह 1: 128-बिट वेक्टर रजिस्टर और SDOT निर्देश। सप्ताह 2: क्लेडीआई माइक्रो-कर्नेल डिस्पेच। सप्ताह 3: कॉर्टेक्स कोर पिनिंग और हीटिंग रोकथाम।",
    "ஆர்ம் நியான் SIMD மற்றும் KleidiAI மைக்ரோ-கெர்னல்கள் (Arm Neon)",
    "நான்கு தண்டவாளங்களில் ஒரே நேரத்தில் புறப்படும் ரயில்களைப் போல, ஆர்ம் நியான் 128-பிட் வெக்டார்கள் ஒரே சுழற்சியில் பல கணக்குகளைச் செய்கின்றன.",
    "வாரம் 1: 128-பிட் பதிவேடுகள் மற்றும் SDOT கட்டளைகள். வாரம் 2: KleidiAI அசெம்பிளி கெர்னல்கள். வாரம் 3: செயல்திறன் கோர் கட்டுப்பாடு.",
    "ఆర్మ్ నియాన్ SIMD & KleidiAI వెక్టర్ కెర్నల్స్ (Arm Neon)",
    "రైల్వే 4-లైన్ల ట్రాక్‌పై ఒకే సిగ్నల్‌తో నాలుగు రైళ్లు ఒకేసారి దూసుకెళ్లినట్లు, ఆర్మ్ నియాన్ ఒకే సైకిల్‌లో 4 డాట్ ప్రొడక్టులను గణిస్తుంది.",
    "వారం 1: 128-బిట్ వెక్టర్ రిజిస్టర్లు & SDOT. వారం 2: KleidiAI అసెంబ్లీ మైక్రో-కెర్నల్స్. వారం 3: బిగ్.లిటిల్ కోర్ పిన్నింగ్.",
    "আর্ম নিয়ন SIMD এবং KleidiAI ভেক্টর কার্নেল (Arm Neon)",
    "ভারতীয় রেলের চার লাইনের ট্র্যাকে একটিমাত্র সবুজ সংকেতে চারটি ট্রেন যেমন একসাথে ছোটে, আর্ম নিয়ন তেমনি এক ক্লিকে বহু হিসাব সম্পন্ন করে।",
    "সপ্তাহ ১: ১২৮-বিট ভেক্টর রেজিস্টার ও SDOT নির্দেশ। সপ্তাহ ২: KleidiAI অ্যাসেম্বলি কার্নেল। সপ্তাহ ৩: বিগ.লিটল কোর পিনিং ও কুলিং।",
    "आर्म निऑन सिमड आणि KleidiAI व्हेक्टर कर्नल्स (Arm Neon)",
    "भारतीय रेल्वेच्या चौपदरी मार्गासारखे हे आहे—एकाच हिरव्या झेंड्यावर चार गाड्या एकाच वेळी धावतात, तसेच निऑन वेक्टर्स एकाच फेरीत काम करतात.",
    "आठवडा १: १२८-बिट रजिस्टर्स आणि SDOT इन्स्ट्रक्शन. आठवडा २: KleidiAI असेंब्ली कर्नल्स. आठवडा ३: कॉर्टेक्स-X कोर पिनिंग आणि कूलिंग."
)

# 4. edge_ai_04_executorch
add_indic(
    "edge_ai_04_executorch",
    "ExecuTorch Runtime & Android Memory Sandboxing",
    "Think of ExecuTorch planned buffers like a modern automated postal sorting center—every parcel has a designated conveyor slot before arriving, guaranteeing zero jams.",
    "Week 1: PTE flatbuffer parsing and ahead-of-time memory plans. Week 2: mmap zero-copy weights vs heap bloat. Week 3: Android JNI bridge profiling.",
    "एग्जीक्यूटरच रनटाइम और एंड्रॉइड मेमोरी सैंडबॉक्सिंग (ExecuTorch)",
    "एग्जीक्यूटरच प्लांड बफर को आधुनिक स्पीड पोस्ट सॉर्टिंग हब की तरह समझें—जहाँ पार्सल आने से पहले ही उसका स्लॉट तय होता है, जिससे कभी जाम नहीं लगता।",
    "सप्ताह 1: PTE फ्लैटबफर और AOT मेमोरी प्लानिंग। सप्ताह 2: mmap जीरो-कॉपी बनाम हीप ब्लोट की रोकथाम। सप्ताह 3: JNI ब्रिज और प्रोसेस RSS प्रोफाइलिंग।",
    "எக்ஸிகியூட்டார்ச் இயக்க நேரம் மற்றும் ஆண்ட்ராய்டு நினைவகம் (ExecuTorch)",
    "தபால் நிலைய வரிசையாக்க மையம் போல, முன்னரே திட்டமிடப்பட்ட நினைவக ஒதுக்கீடு ஆண்ட்ராய்டில் செயலியை முடங்காமல் பாதுகாக்கிறது.",
    "வாரம் 1: PTE வடிவமைப்பு மற்றும் AOT நினைவகம். வாரம் 2: mmap பூஜ்ஜிய-நகல் மேலாண்மை. வாரம் 3: JNI மற்றும் ஆண்ட்ராய்டு நினைவக ஆய்வு.",
    "ఎగ్జిక్యూటర్‌చ్ రన్‌టైమ్ & ఆండ్రాయిడ్ మెమరీ పరిమితులు (ExecuTorch)",
    "ముందుగానే నిర్ణయించిన పోస్టల్ సార్టింగ్ కన్వేయర్ బెల్ట్ లాంటిది—మెమరీ మొత్తం ముందే ప్లాన్ చేసి ఉండటం వల్ల యాప్ ఎప్పటికీ క్రాష్ అవ్వదు.",
    "వారం 1: PTE ఫ్లాట్‌బఫర్ & మెమరీ ప్లానింగ్. వారం 2: mmap జీరో-కాపీ పద్ధతి. వారం 3: JNI బ్రిడ్జ్ మరియు ఆండ్రాయిడ్ RAM ప్రొఫైలింగ్.",
    "এক্সিকিউটর্চ রানটাইম এবং অ্যান্ড্রয়েড মেমরি নিয়ন্ত্রণ (ExecuTorch)",
    "স্পিড পোস্টের স্বয়ংক্রিয় বাছাই কেন্দ্রের মতো, যেখানে পার্সেল আসার আগেই তার ট্রে নির্দিষ্ট থাকে, এক্সিকিউটর্চ মেমরি আগে থেকেই সাজিয়ে রাখে।",
    "সপ্তাহ ১: PTE ফ্ল্যাটবাফার ও AOT মেমরি পরিকল্পনা। সপ্তাহ ২: mmap জিরো-কপি কৌশল। সপ্তাহ ৩: JNI ব্রিজ ও অ্যান্ড্রয়েড RAM প্রোফাইলিং।",
    "एक्झिक्युटरच रनटाईम आणि अँड्रॉइड मेमरी व्यवस्थापन (ExecuTorch)",
    "टपाल खात्याच्या आधुनिक वर्गीकरण केंद्रासारखे हे आहे—पार्सल येण्यापूर्वीच त्याची जागा ठरलेली असते, त्यामुळे कधीही गोंधळ किंवा विलंब होत नाही.",
    "आठवडा १: PTE फ्लॅटबफर आणि पूर्व-नियोजित मेमरी. आठवडा २: mmap झिरो-कॉपी व्यवस्थापन. आठवडा ३: JNI ब्रिज आणि अँड्रॉइड RAM प्रोफाइलिंग."
)

# 5. edge_ai_05_kvcache
add_indic(
    "edge_ai_05_kvcache",
    "Attention Sinks, Ring Buffer KV-Cache & Latency Optimization",
    "Think of a sink-token KV-cache like a traditional merchant's sliding ledger—foundational opening balance permanently anchored on page 1, current day transactions recorded in a rolling window.",
    "Week 1: Softmax attention collapse and attention sink tokens. Week 2: Sliding-window ring buffer implementation. Week 3: Bounding resident RAM strictly <1.8 GB.",
    "अटेंशन सिंक और रिंग बफर KV-कैश प्रबंधन (KV-Cache)",
    "सिंक-टोकन KV-कैश को मुनीमजी के बही-खाते की तरह समझें—जहाँ पहली पृष्ठ पर मुख्य जमा पूंजी हमेशा स्थिर रहती है और केवल हाल के सौदे घूमते पन्ने पर लिखे जाते हैं।",
    "सप्ताह 1: सॉफ्टमैक्स अटेंशन सिंक और प्रारंभिक 4 टोकन। सप्ताह 2: स्लाइडिंग-विंडो रिंग बफर। सप्ताह 3: प्रोसेस VmRSS को 1800MB के अंदर सुरक्षित रखना।",
    "அட்டென்ஷன் சிங்க் மற்றும் ரிங் பஃபர் KV-கேச் (KV-Cache Optimization)",
    "பாரம்பரிய கணக்கு ஏடு போல, தொடக்க மூலதனம் முதல் பக்கத்தில் நிரந்தரமாக இருக்கும், சமீபத்திய வரவு-செலவுகள் சுழலும் ஏட்டில் குறிக்கப்படும்.",
    "வாரம் 1: அட்டென்ஷன் சிங்க் டோக்கன்கள். வாரம் 2: சுழலும் ரிங் பஃபர் KV-கேச். வாரம் 3: 1.8 GB RAM வரம்புக்குள் நிலைநிறுத்துதல்.",
    "అటెన్షన్ సింక్స్ & రింగ్ బఫర్ KV-కాష్ నిర్వహణ (KV-Cache)",
    "వ్యాపారి ఖాతా పుస్తకం లాంటిది—మొదటి పేజీలో ప్రాథమిక నిల్వ స్థిరంగా ఉంటుంది, ఇటీవలి లావాదేవీలు మాత్రమే తిరిగే పేజీల్లో రికార్డ్ అవుతాయి.",
    "వారం 1: అటెన్షన్ సింక్ సూత్రాలు. వారం 2: స్లైడింగ్-విండో రింగ్ బఫర్. వారం 3: RAM 1.8 GB లోపలే ఉండేలా నియంత్రణ.",
    "অ্যাটেনশন সিঙ্ক এবং রিং বাফার KV-ক্যাশ ব্যবস্থাপনা (KV-Cache)",
    "মহাজনের খাতার মতো—প্রথম পৃষ্ঠার মূল হিসাবটি সর্বদা স্থির থাকে, আর সাম্প্রতিক লেনদেনগুলো কেবল ঘূর্ণায়মান পাতায় লেখা হয়।",
    "সপ্তাহ ১: সফটম্যাক্স অ্যাটেনশন সিঙ্ক টোকেন। সপ্তাহ ২: স্লাইডিং উইন্ডো রিং বাফার। সপ্তাহ ৩: RAM ১৮০০ মেগাবাইটের নিচে সীমাবদ্ধ রাখা।",
    "अटेंशन सिंक आणि रिंग बफर KV-कॅश ऑप्टिमायझेशन (KV-Cache)",
    "व्यापाऱ्याच्या चालू खतावणीसारखे हे आहे—पहिल्या पानावरचे मूळ भांडवल कायम राहते आणि चालू व्यवहारांची नोंद फिरत्या पानांवर होते.",
    "आठवडा १: अटेंशन सिंक टोकन्सचे महत्त्व. आठवडा २: रिंग बफर KV-कॅश. आठवडा ३: प्रोसेस RAM १८०० MB च्या खाली कायम राखणे."
)

# -----------------------------------------------------------------------------
# DOMAIN 2: Exam Preparation (GATE CS & Algorithms)
# -----------------------------------------------------------------------------
add_indic(
    "gate_01_dsa",
    "GATE CS Data Structures & Algorithms",
    "Think of an AVL tree like an old weighing scale with two balancing pans—if one pan tips by more than one measure, you immediately rotate the weights.",
    "Week 1: Asymptotic complexity and recurrence relations. Week 2: Balanced BSTs (AVL, Red-Black). Week 3: Dijkstra and Dynamic Programming.",
    "गेट सीएस: डेटा स्ट्रक्चर्स और एल्गोरिदम (GATE DSA)",
    "एवीएल (AVL) ट्री को पुराने तराजू की तरह समझें—अगर एक पलड़ा एक सीमा से ज्यादा झुक जाए, तो तुरंत वजन बदलकर संतुलन बहाल कर दिया जाता है।",
    "सप्ताह 1: ऐसिम्प्टोटिक टाइम कॉम्प्लेक्सिटी। सप्ताह 2: संतुलित ट्री (AVL और Red-Black)। सप्ताह 3: डायक्स्ट्रा और डायनामिक प्रोग्रामिंग।",
    "கேட் சிஎஸ்: தரவு கட்டமைப்புகள் மற்றும் வழிமுறைகள் (GATE DSA)",
    "AVL மரத்தை ஒரு இரு-தட்டு தராசு போலக் கருதலாம்—ஒரு தட்டு கூடுதலாக சாய்ந்தால் உடனடியாக சுழற்சி மூலம் சமநிலைப்படுத்தப்படுகிறது.",
    "வாரம் 1: அல்காரிதம் சிக்கலான தன்மை. வாரம் 2: சமச்சீர் மரங்கள். வாரம் 3: டைக்ஸ்ட்ரா மற்றும் டைனமிக் புரோகிராமிங்.",
    "గేట్ సిఎస్: డేటా స్ట్రక్చర్స్ & అల్గోరిథంలు (GATE DSA)",
    "AVL ట్రీని బ్యాలెన్సింగ్ త్రాసుతో పోల్చవచ్చు—ఒక వైపు బరువు ఎక్కువైతే వెంటనే రొటేషన్ ద్వారా బ్యాలెన్స్ చేస్తుంది.",
    "వారం 1: టైమ్ కాంప్లెక్సిటీ సూత్రాలు. వారం 2: బ్యాలెన్స్‌డ్ ట్రీలు. వారం 3: డైక్స్‌ట్రా & డైనమిక్ ప్రోగ్రామింగ్.",
    "গেট সিএস: ডেটা স্ট্রাকচার ও অ্যালগরিদম (GATE DSA)",
    "এভিএল ট্রিকে দুই পাল্লার দাঁড়িপাল্লার মতো ভাবুন—এক পাল্লা বেশি ঝুঁকে পড়লে সাথে সাথে ঘুরিয়ে ভারসাম্য ফিরিয়ে আনা হয়।",
    "সপ্তাহ ১: অ্যাসিম্পটোটিক জটিলতা। সপ্তাহ ২: সুষম ট্রি (AVL)। সপ্তাহ ৩: ডায়াকস্ট্রা ও ডায়নামিক প্রোগ্রামিং।",
    "गेट सीएस: डेटा स्ट्रक्चर्स आणि अल्गोरिदम (GATE DSA)",
    "AVL ट्री म्हणजे दोन पारड्यांचा तराजू—एका बाजूला जास्त कल झाला की लगेच रोटेशन करून समतोल साधला जातो.",
    "आठवडा १: टाइम कॉम्प्लेक्सिटी. आठवडा २: संतुलित ट्री रचना. आठवडा ३: डायकस्ट्रा आणि डायनॅमिक प्रोग्रॅमिंग."
)

add_indic(
    "gate_02_os",
    "Operating Systems: Synchronization & Memory Paging",
    "Think of semaphores like the physical brass tokens used at busy South Indian tiffin rooms—only holders of a token can enter the serving counter.",
    "Week 1: Semaphores, mutexes, and critical sections. Week 2: Deadlock conditions and Banker's algorithm. Week 3: Virtual memory and page replacement.",
    "ऑपरेटिंग सिस्टम: सिंक्रोनाइजेशन और पेजिंग (Operating Systems)",
    "सेमाफोर को दक्षिण भारतीय भोजनालय के पीतल के टोकन की तरह समझें—जिसके पास टोकन है केवल वही भोजन काउंटर तक पहुँच सकता है।",
    "सप्ताह 1: सेमाफोर, म्यूटेक्स और क्रिटिकल सेक्शन। सप्ताह 2: डेडलॉक और बैंकर्स एल्गोरिदम। सप्ताह 3: वर्चुअल मेमोरी और पेज रिप्लेसमेंट।",
    "இயக்க முறைமைகள்: ஒத்திசைவு மற்றும் நினைவகம் (Operating Systems)",
    "செமாஃபோர் என்பது உணவகங்களில் வரிசைக்கான டோக்கன் போன்றது—டோக்கன் வைத்திருப்பவர் மட்டுமே கவுண்ட்டரை அணுக முடியும்.",
    "வாரம் 1: செமாஃபோர் மற்றும் மியூடெக்ஸ். வாரம் 2: டெட்லாக் தவிர்ப்பு. வாரம் 3: பக்க மாற்று வழிமுறைகள்.",
    "ఆపరేటింగ్ సిస్టమ్స్: ప్రాసెస్ సింక్రొనైజేషన్ (Operating Systems)",
    "సెమాఫోర్‌ను హోటళ్లలో ఇచ్చే టోకెన్‌తో పోల్చవచ్చు—టోకెన్ ఉన్న వ్యక్తికి మాత్రమే సర్వింగ్ కౌంటర్ ప్రవేశం ఉంటుంది.",
    "వారం 1: సెమాఫోర్లు & మ్యూటెక్స్. వారం 2: డెడ్‌లాక్ పరిష్కారాలు. వారం 3: వర్చువల్ మెమరీ పేజింగ్.",
    "অপারেটিং সিস্টেম: প্রসেস সমন্বয় ও পেজিং (Operating Systems)",
    "সেমাফোরকে মিষ্টির দোকানের টোকেন ব্যবস্থার মতো ভাবতে পারেন—টোকেন থাকলেই কেবল কাউন্টারে গিয়ে খাবার নেওয়া যায়।",
    "সপ্তাহ ১: সেমাফোর ও মিউটেক্স। সপ্তাহ ২: ডেডলক ও ব্যাংকার্স অ্যালগরিদম। সপ্তাহ ৩: ভার্চুয়াল মেমরি ও পেজ রিপ্লেসমেন্ট।",
    "ऑपरेटिंग सिस्टम: सिंक्रोनायझेशन आणि पेजिंग (Operating Systems)",
    "सेमाफोर म्हणजे उपहारगृहातील टोकन पद्धतीसारखे आहे—टोकन असेल तरच काउंटरवर सेवा मिळते, ज्यामुळे गोंधळ टळतो.",
    "आठवडा १: सेमाफोर आणि क्रिटिकल सेक्शन. आठवडा २: डेडलॉक आणि बँकर्स अल्गोरिदम. आठवडा ३: व्हर्च्युअल मेमरी आणि पेजिंग."
)

add_indic(
    "gate_03_dbms",
    "DBMS: Normalization, ACID & Transaction Protocols",
    "Think of ACID transactions like buying a confirmed Tatkal train ticket on IRCTC—either money is deducted AND seat is confirmed, or full refund occurs with zero state corruption.",
    "Week 1: Relational algebra and SQL queries. Week 2: Normalization (3NF and BCNF). Week 3: Transaction serializability and Strict 2PL.",
    "डेटाबेस प्रणालियाँ: नॉर्मलाइजेशन और ACID (DBMS)",
    "ACID ट्रांजेक्शन को IRCTC पर तत्काल टिकट बुकिंग की तरह समझें—या तो पैसा कटेगा और सीट पक्की होगी, या फिर पैसे वापस आ जाएंगे; कभी बीच में फंसा नहीं रहेगा।",
    "सप्ताह 1: रिलेशनल अलजेब्रा और जटिल SQL। सप्ताह 2: नॉर्मलाइजेशन (3NF और BCNF)। सप्ताह 3: ट्रांजेक्शन सीरियलाइजेबिलिटी और स्ट्रिक्ट 2PL।",
    "தரவுத்தள அமைப்புகள்: இயல்பாக்கம் மற்றும் ACID (DBMS)",
    "ACID பரிவர்த்தனை என்பது ரயில் தட்கல் முன்பதிவு போன்றது—பணம் பிடித்தம் செய்யப்பட்டு இருக்கை உறுதி செய்யப்படும் அல்லது பணம் முழுமையாகத் திரும்பப் பெறப்படும்.",
    "வாரம் 1: SQL வினவல்கள். வாரம் 2: இயல்பாக்கம் (BCNF). வாரம் 3: 2PL நெறிமுறைகள்.",
    "డేటాబేస్ మేనేజ్మెంట్: నార్మలైజేషన్ & ACID (DBMS)",
    "ACID లావాదేవీని తత్కాల్ రైలు టికెట్ బుకింగ్‌తో పోల్చవచ్చు—డబ్బులు కట్ అయ్యి టికెట్ అయినా కన్ఫర్మ్ అవుతుంది, లేదా డబ్బులు వెనక్కి వచ్చేస్తాయి.",
    "వారం 1: రిలేషనల్ ఆల్జీబ్రా. వారం 2: నార్మలైజేషన్ (BCNF). వారం 3: ట్రాన్సాక్షన్ ఐసోలేషన్ & 2PL.",
    "ডেটাবেস ম্যানেজমেন্ট: নরমালাইজেশন ও ACID (DBMS)",
    "ACID ট্রানজ্যাকশনকে ট্রেনের তৎকাল টিকিট কাটার মতো ভাবুন—টাকা কাটলে টিকিট কনফার্ম হবে, নাহলে পুরো টাকা ফেরত আসবে; কোনো অসঙ্গতি থাকবে না।",
    "সপ্তাহ ১: রিলেশনাল অ্যালজেব্রা ও SQL। সপ্তাহ ২: নরমালাইজেশন (BCNF)। সপ্তাহ ৩: স্ট্রিক্ট 2PL প্রোটোকল।",
    "डेटाबेस मॅनेजमेंट: नॉर्मलायझेशन आणि ACID (DBMS)",
    "ACID व्यवहार म्हणजे रेल्वेचे तत्काळ तिकीट बुक करण्यासारखा आहे—एकतर पैसे कटून तिकीट मिळते किंवा पैसे पूर्ण परत मिळतात.",
    "आठवडा १: रिलेशनल अल्जेब्रा. आठवडा २: नॉर्मलायझेशन (BCNF). आठवडा ३: स्ट्रिक्ट 2PL व ट्रान्झॅक्शन सुरक्षा."
)

# -----------------------------------------------------------------------------
# DOMAIN 3: Certification Paths (Cloud Architecture & DevSecOps)
# -----------------------------------------------------------------------------
add_indic(
    "cert_01_cloud_sec",
    "Cloud Security Architecture & Zero Trust IAM",
    "Think of Zero Trust like a high-security research laboratory—holding an entry badge doesn't unlock every door; biometric scans are required for every individual vault.",
    "Week 1: Identity and Access Management (IAM) and Least Privilege. Week 2: Mutual TLS and Zero Trust architecture. Week 3: KMS envelope encryption.",
    "क्लाउड सुरक्षा और जीरो-ट्रस्ट आईएएम (Cloud Security)",
    "जीरो ट्रस्ट को उच्च सुरक्षा वाले अनुसंधान केंद्र की तरह समझें—जहाँ मुख्य द्वार का पास होने पर भी हर एक कमरे के लिए अलग बायोमेट्रिक सत्यापन आवश्यक होता है।",
    "सप्ताह 1: IAM भूमिकाएँ और न्यूनतम विशेषाधिकार। सप्ताह 2: म्युचुअल टीएलएस और जीरो-ट्रस्ट फ्रेमवर्क। सप्ताह 3: KMS एनवेलप एन्क्रिप्शन।",
    "கிளவுட் பாதுகாப்பு மற்றும் ஜீரோ டிரஸ்ட் (Cloud Security)",
    "ஜீரோ டிரஸ்ட் என்பது உயர் பாதுகாப்பு ஆய்வகம் போன்றது—முக்கிய கதவைத் திறந்தாலும் ஒவ்வொரு அறைக்கும் தனித்தனியாக சரிபார்ப்பு தேவைப்படுகிறது.",
    "வாரம் 1: IAM மற்றும் குறைந்தபட்ச உரிமை. வாரம் 2: ஜீரோ டிரஸ்ட் நெறிமுறைகள். வாரம் 3: KMS குறியாக்கவியல்.",
    "క్లౌడ్ సెక్యూరిటీ & జీరో ట్రస్ట్ IAM (Cloud Security)",
    "జీరో ట్రస్ట్‌ను హై-సెక్యూరిటీ పరిశోధనా కేంద్రంతో పోల్చవచ్చు—ప్రధాన గేట్ దాటినప్పటికీ ప్రతి గదికీ ప్రత్యేక అనుమతి తప్పనిసరి.",
    "వారం 1: IAM & కనీస హక్కుల విధానం. వారం 2: జీరో ట్రస్ట్ ఆర్కిటెక్చర్. వారం 3: KMS ఎన్క్రిప్షన్.",
    "ক্লাউড সিকিউরিটি ও জিরো ট্রাস্ট IAM (Cloud Security)",
    "জিরো ট্রাস্টকে একটি উচ্চ-নিরাপত্তার গবেষণাগারের মতো ভাবুন—বাইরের গেট পার হলেও ভেতরের প্রতিটি ঘরের জন্য আলাদা বায়োমেট্রিক লাগে।",
    "সপ্তাহ ১: IAM ও সর্বনিম্ন অধিকার নীতি। সপ্তাহ ২: জিরো ট্রাস্ট স্থাপত্য। সপ্তাহ ৩: KMS খাম এনক্রিপশন।",
    "क्लाउड सुरक्षा आणि झिरो ट्रस्ट IAM (Cloud Security)",
    "झिरो ट्रस्ट म्हणजे कडक सुरक्षेची प्रयोगशाळा—प्रवेशद्वार ओलांडले तरी प्रत्येक खोलीसाठी स्वतंत्र ओळख पटवावी लागते.",
    "आठवडा १: IAM आणि कमीत कमी परवानग्या. आठवडा २: झिरो ट्रस्ट मॉडेल. आठवडा ३: KMS एन्क्रिप्शन तंत्रज्ञान."
)

add_indic(
    "cert_02_k8s_containers",
    "Kubernetes Cluster Orchestration & Pod Networking",
    "Think of Kubernetes Pods like cargo containers on a modern container ship—standardized, isolated, and automatically restacked by cranes if one is damaged.",
    "Week 1: Pod lifecycles, ConfigMaps, and Secrets. Week 2: Service discovery (ClusterIP vs LoadBalancer). Week 3: Horizontal Pod Autoscaler (HPA).",
    "कुबरनेट्स क्लस्टर और पॉड नेटवर्किंग (Kubernetes)",
    "कुबरनेट्स पॉड्स को मालवाहक जहाज के कंटेनरों की तरह समझें—जो मानकीकृत और सुरक्षित होते हैं, और एक खराब होने पर क्रेन तुरंत दूसरा रख देती है।",
    "सप्ताह 1: पॉड लाइफसाइकिल और कॉन्फ़िगमैप। सप्ताह 2: क्लस्टरआईपी और सर्विस नेटवर्किंग। सप्ताह 3: हॉरिजॉन्टल पॉड ऑटोस्केलर (HPA)।",
    "குபர்னெட்டீஸ் மற்றும் கண்டெய்னர்கள் (Kubernetes)",
    "குபர்னெட்டீஸ் என்பது சரக்குக் கப்பலில் உள்ள பெட்டிகள் போன்றது—சேதமடைந்தால் தானாகவே மாற்று பெட்டி நிறுவப்படுகிறது.",
    "வாரம் 1: பாட் வாழ்க்கைச் சுழற்சி. வாரம் 2: சேவை கண்டுபிடிப்பு (ClusterIP). வாரம் 3: தானியங்கி அளவீடு (HPA).",
    "కుబర్నెటిస్ క్లస్టర్ ఆర్కెస్ట్రేషన్ (Kubernetes)",
    "కుబర్నెటిస్ పాడ్స్‌ను కార్గో షిప్ కంటైనర్లతో పోల్చవచ్చు—ఒకటి దెబ్బతిన్నా వెంటనే క్రేన్ ద్వారా కొత్తది ఏర్పాటు చేయబడుతుంది.",
    "వారం 1: పాడ్ లైఫ్‌సైకిల్ & కాన్ఫిగరేషన్లు. వారం 2: సర్వీస్ రకాలు (ClusterIP). వారం 3: ఆటో-స్కేలింగ్ (HPA).",
    "কুবারনেটিস ও কনটেইনার ব্যবস্থাপনা (Kubernetes)",
    "কুবারনেটিস পডকে মালবাহী জাহাজের কনটেইনারের মতো ভাবুন—একটি নষ্ট হলে ক্রেন দিয়ে সাথে সাথে আরেকটি প্রতিস্থাপন করা হয়।",
    "সপ্তাহ ১: পড লাইফসাইকেল ও কনফিগম্যাপ। সপ্তাহ ২: সার্ভিস ডিসকভারি। সপ্তাহ ৩: অনুভূমিক অটোস্কেলিং (HPA)।",
    "कुबरनेटीस आणि कंटेनर व्यवस्थापन (Kubernetes)",
    "कुबरनेटीस पॉड्स म्हणजे मालवाहू जहाजावरील कंटेनर्ससारखे आहेत—एक खराब झाला तरी क्रेन लगेच दुसरा नवीन कंटेनर आणून ठेवते.",
    "आठवडा १: पॉड रचना आणि कॉन्फिग. आठवडा २: सर्व्हिस डिस्कव्हरी (ClusterIP). आठवडा ३: ऑटो-स्केलिंग (HPA)."
)

add_indic(
    "cert_03_devops_ci",
    "Enterprise CI/CD Pipelines & Terraform IaC",
    "Think of Terraform state locking like the synchronized ledger of a bank vault—two officers cannot turn their keys at once without corrupting the register.",
    "Week 1: GitHub Actions pipelines and automated testing. Week 2: Terraform remote state and distributed locks. Week 3: Canary releases and immutable infra.",
    "डेवऑप्स सीआई/सीडी और टेराफॉर्म (DevOps CI/CD)",
    "टेराफॉर्म स्टेट लॉकिंग को बैंक के मुख्य वॉल्ट के रजिस्टर की तरह समझें—जहाँ दो अधिकारी एक साथ प्रविष्टि नहीं कर सकते ताकि रिकॉर्ड शुद्ध रहे।",
    "सप्ताह 1: गिटहब एक्शन्स और ऑटोमेटेड टेस्ट्स। सप्ताह 2: टेराफॉर्म रिमोट स्टेट और लॉकिंग। सप्ताह 3: कैनरी डिप्लॉयमेंट और इम्यूटेबल इन्फ्रा।",
    "டெவ்ஆப்ஸ் சிஐ/சிடி மற்றும் டெர்ராஃபார்ம் (DevOps CI/CD)",
    "டெர்ராஃபார்ம் பூட்டுதல் என்பது வங்கிப் பெட்டகத்தின் பதிவேடு போன்றது—இருவர் ஒரே நேரத்தில் மாற்றுவதைத் தடுக்கிறது.",
    "வாரம் 1: GitHub Actions பைப்லைன்கள். வாரம் 2: டெர்ராஃபார்ம் நிலை மற்றும் பூட்டுகள். வாரம் 3: கேனரி வெளியீடு.",
    "డెవ్‌ఆప్స్ CI/CD & టెర్రాఫామ్ (DevOps CI/CD)",
    "టెర్రాఫామ్ స్టేట్ లాకింగ్‌ను బ్యాంక్ లాకర్ రిజిస్టర్‌తో పోల్చవచ్చు—ఇద్దరు వ్యక్తులు ఒకేసారి మార్పులు చేయకుండా లాక్ చేస్తుంది.",
    "వారం 1: GitHub Actions పైప్‌లైన్లు. వారం 2: టెర్రాఫామ్ రిమోట్ స్టేట్ లాక్స్. వారం 3: కెనరీ రిలీజ్ పద్ధతులు.",
    "ডেভঅপ্স CI/CD এবং টেরাফর্ম (DevOps CI/CD)",
    "টেরাফর্ম স্টেট লকিংকে ব্যাংক ভল্টের যৌথ চাবির মতো ভাবুন—দুজনে একসাথে লিখে খাতা নষ্ট করার কোনো সুযোগ নেই।",
    "সপ্তাহ ১: GitHub Actions অটোমেশন। সপ্তাহ ২: টেরাফর্ম রিমোট স্টেট ও লকিং। সপ্তাহ ৩: ক্যানারি ডেপ্লয়মেন্ট।",
    "डेव्हऑप्स CI/CD आणि टेराफॉर्म (DevOps CI/CD)",
    "टेराफॉर्म लॉकिंग म्हणजे बँकेच्या तिजोरीच्या नोंदवहीसारखे आहे—दोन व्यक्तींनी एकाच वेळी खाडाखोड करू नये म्हणून कुलूप लावले जाते.",
    "आठवडा १: GitHub Actions ऑटोमेशन. आठवडा २: टेराफॉर्म स्टेट आणि लॉक्स. आठवडा ३: कॅनरी डिप्लॉयमेंट आणि सुरक्षा."
)

# -----------------------------------------------------------------------------
# DOMAIN 4: Career Guidance (Data Engineering & Tech Transitions)
# -----------------------------------------------------------------------------
add_indic(
    "career_01_data_analyst",
    "SQL Analytics & Business Intelligence",
    "Think of SQL window functions like inspecting a running ledger while keeping the total balance in the corner of your eye on every single entry.",
    "Week 1: Window functions (ROW_NUMBER, RANK, LAG). Week 2: Common Table Expressions (CTEs). Week 3: Cohort retention analysis.",
    "एसक्यूएल एनालिटिक्स और बिजनेस इंटेलिजेंस (SQL Analytics)",
    "एसक्यूएल विंडो फंक्शन्स को बही-खाते के उस पन्ने की तरह समझें—जहाँ हर पंक्ति का हिसाब करते हुए भी कोने में पूरे महीने का कुल जोड़ दिखता रहता है।",
    "सप्ताह 1: विंडो फंक्शन्स (RANK, LAG, LEAD)। सप्ताह 2: कॉमन टेबल एक्सप्रेशन्स (CTEs)। सप्ताह 3: कोहोर्ट रिटेंशन एनालिसिस।",
    "SQL பகுப்பாய்வு மற்றும் வணிக நுண்ணறிவு (SQL Analytics)",
    "SQL விண்டோ செயல்பாடுகள் என்பது ஒவ்வொரு கணக்கையும் பார்க்கும் போதே ஒட்டுமொத்த தொகையையும் ஒருசேர கவனிப்பது போன்றது.",
    "வாரம் 1: விண்டோ செயல்பாடுகள் (RANK, LAG). வாரம் 2: CTE வெளிப்பாடுகள். வாரம் 3: பயனர் தக்கவைப்பு பகுப்பாய்வு.",
    "SQL అనలిటిక్స్ & బిజినెస్ ఇంటెలిజెన్స్ (SQL Analytics)",
    "SQL విండో ఫంక్షన్లను ఖాతా పుస్తకం సరిచూడటంతో పోల్చవచ్చు—ప్రతి లావాదేవీని చూస్తూనే మొత్తం నిల్వను ఒకేసారి గమనించవచ్చు.",
    "వారం 1: విండో ఫంక్షన్లు (RANK, LAG). వారం 2: CTEs రాయడం. వారం 3: కోహోర్ట్ విశ్లేషణ.",
    "SQL অ্যানালিটিক্স এবং বিজনেস ইন্টেলিজেন্স (SQL Analytics)",
    "SQL উইন্ডো ফাংশনকে খাতার পাতার মতো ভাবুন—যেখানে প্রতি লাইনের হিসাব করার সময়ও মোট যোগফল চোখের সামনে দেখা যায়।",
    "সপ্তাহ ১: উইন্ডো ফাংশন (RANK, LAG)। সপ্তাহ ২: CTE এর ব্যবহার। সপ্তাহ ৩: গ্রাহক ধরে রাখার বিশ্লেষণ।",
    "SQL ॲनालिटिक्स आणि बिझनेस इंटेलिजन्स (SQL Analytics)",
    "विंडो फंक्शन्स म्हणजे जमा-खर्चाच्या वहीत प्रत्येक ओळीची नोंद पाहतानाच संपूर्ण महिन्याची बेरीज डोळ्यासमोर असण्यासारखे आहे.",
    "आठवडा १: विंडो फंक्शन्स (RANK, LAG). आठवडा २: CTEs चा वापर. आठवडा ३: व्यवसाय ग्राहक टिकवण विश्लेषण."
)

add_indic(
    "career_02_data_pipeline",
    "Data Warehousing & Orchestration Pipelines",
    "Think of a Star Schema warehouse like a busy vegetable market—the central billing counter (Fact) is connected directly to outer vegetable stalls (Dimensions).",
    "Week 1: Kimball dimensional modeling and Star Schema. Week 2: SCD Type 2 historical tracking. Week 3: Apache Airflow DAGs.",
    "डेटा वेयरहाउसिंग और पाइपलाइन निर्माण (Data Pipelines)",
    "स्टार स्कीमा को एक बड़े सब्जी बाजार की तरह समझें—जहाँ बीच का मुख्य बिलिंग काउंटर (Fact) सीधे आसपास की दुकानों (Dimensions) से जुड़ा होता है।",
    "सप्ताह 1: स्टार स्कीमा और फैक्ट टेबल डिजाइन। सप्ताह 2: SCD टाइप 2 ऐतिहासिक ऑडिट। सप्ताह 3: अपाचे एयरफ्लो DAG वर्कफ़्लो।",
    "தரவு சேமிப்பு மற்றும் பைப்லைன்கள் (Data Pipelines)",
    "ஸ்டார் ஸ்கீமா என்பது ஒரு பெரிய சந்தை போன்றது—மைய பில்லிங் கவுண்டர் அனைத்து கடை பிரிவுகளுடனும் நேரடியாக இணைக்கப்பட்டுள்ளது.",
    "வாரம் 1: ஸ்டார் ஸ்கீமா வடிவமைப்பு. வாரம் 2: SCD வகை 2 வரலாறு. வாரம் 3: Apache Airflow பணிப்பாய்வு.",
    "డేటా వేర్‌హౌసింగ్ & పైప్‌లైన్స్ (Data Pipelines)",
    "స్టార్ స్కీమాను కూరగాయల మార్కెట్‌తో పోల్చవచ్చు—మధ్యలో ఉండే బిల్లింగ్ కౌంటర్ అన్ని దుకాణాలతో నేరుగా అనుసంధానించబడి ఉంటుంది.",
    "వారం 1: స్టార్ స్కీమా నిర్మాణం. వారం 2: SCD టైప్ 2 హిస్టరీ ట్రాకింగ్. వారం 3: అపాచీ ఎయిర్‌ఫ్లో DAGs.",
    "ডেটা ওয়্যারহাউসিং ও পাইপলাইন তৈরি (Data Pipelines)",
    "স্টার স্কিমাকে একটি পাইকারি বাজারের মতো ভাবুন—মাঝখানের ক্যাশ কাউন্টারটি চারপাশের সব দোকানের সাথে সরাসরি যুক্ত।",
    "সপ্তাহ ১: স্টার স্কিমা ও ফ্যাক্ট টেবিল। সপ্তাহ ২: SCD টাইপ ২ ইতিহাস সংরক্ষণ। সপ্তাহ ৩: অ্যাপাচি এয়ারফ্লো অটোমেশন।",
    "डेटा वेअरहाऊसिंग आणि पाइपलाइन निर्मिती (Data Pipelines)",
    "स्टार स्कीमा म्हणजे भाजी मार्केटसारखे आहे—मध्यभागी मुख्य बिलिंग केंद्र (Fact) आणि आजूबाजूला स्वतंत्र दुकाने (Dimensions) असतात.",
    "आठवडा १: स्टार स्कीमा डिझाइन. आठवडा २: SCD टाइप २ इतिहास नोंदणी. आठवडा ३: अपाचे एअरफ्लो DAG शेड्युलिंग."
)

add_indic(
    "career_03_interview_prep",
    "Senior Tech Interview System Design & Case Studies",
    "Think of Consistent Hashing like a circular roundabout of toll gates—if one gate closes, cars simply glide forward to the very next gate without rerouting the whole city.",
    "Week 1: CAP theorem and distributed trade-offs. Week 2: Consistent hashing and caching layers. Week 3: Rate limiting and STAR interview frameworks.",
    "सीनियर टेक इंटरव्यू: सिस्टम डिजाइन केस स्टडीज (System Design)",
    "कंसिस्टेंट हैशिंग को एक गोल चक्कर (राउंडअबाउट) के टोल गेट्स की तरह समझें—अगर एक गेट बंद हो, तो गाड़ियाँ शहर जाम किए बिना तुरंत अगले गेट पर चली जाती हैं।",
    "सप्ताह 1: सीएपी (CAP) प्रमेय और डिस्ट्रीब्यूटेड विकल्प। सप्ताह 2: कंसिस्टेंट हैशिंग और कैशिंग। सप्ताह 3: रेट लिमिटिंग और सिस्टम डिजाइन इंटरव्यू।",
    "கணினி வடிவமைப்பு மற்றும் நேர்காணல் தயாரிப்பு (System Design)",
    "சீரான ஹாஷிங் என்பது ஒரு வட்டச் சாலை போன்றது—ஒரு பாதை மூடப்பட்டால் போக்குவரத்து நெரிசல் இல்லாமல் அடுத்த பாதைக்குத் திருப்பப்படுகிறது.",
    "வாரம் 1: CAP தேற்றம். வாரம் 2: சீரான ஹாஷிங் மற்றும் கேச்சிங். வாரம் 3: ஏபிஐ வரம்புகள் மற்றும் நேர்காணல்.",
    "సిస్టమ్ డిజైన్ & ఇంటర్వ్యూ ప్రిపరేషన్ (System Design)",
    "కన్సిస్టెంట్ హ్యాషింగ్‌ను రౌండ్‌ఎబౌట్ టోల్ గేట్లతో పోల్చవచ్చు—ఒక గేట్ మూతబడినా ట్రాఫిక్ ఆగకుండా తర్వాతి గేట్ వైపు సాఫీగా సాగుతుంది.",
    "వారం 1: CAP సిద్ధాంతం. వారం 2: కన్సిస్టెంట్ హ్యాషింగ్. వారం 3: రేట్ లిమిటింగ్ & సిస్టమ్ డిజైన్.",
    "সিস্টেম ডিজাইন ও ইন্টারভিউ প্রস্তুতি (System Design)",
    "কনসিস্টেন্ট হ্যাশিংকে একটি গোলচত্বরের টোল প্লাজার মতো ভাবুন—একটি গেট বন্ধ হলেও বাকি সব গাড়ি জ্যাম না লাগিয়ে পরের গেটে চলে যায়।",
    "সপ্তাহ ১: CAP থিওরেম। সপ্তাহ ২: কনসিস্টেন্ট হ্যাশিং ও ক্যাশিং। সপ্তাহ ৩: রেট লিমিটার ও সিস্টেম ডিজাইন ইন্টারভিউ।",
    "सिस्टम डिझाइन आणि तंत्रज्ञान मुलाखत तयारी (System Design)",
    "कंसिस्टंट हॅशिंग म्हणजे गोल ट्रॅफिक जंक्शनसारखे आहे—एक रस्ता बंद झाला तरी संपूर्ण शहर न थांबवता गाड्या आपोआप पुढच्या रस्त्याला लागतात.",
    "आठवडा १: CAP थिअरी. आठवडा २: कन्सिसटंट हॅशिंग व कॅशिंग. आठवडा ३: रेट लिमिटर आणि मुलाखतीचे तंत्र."
)

# -----------------------------------------------------------------------------
# DOMAIN 5: Skill Development (Applied AI & Python)
# -----------------------------------------------------------------------------
add_indic(
    "skill_01_python_ds",
    "Python for Data Science: NumPy & Pandas",
    "Think of NumPy vectorized operations like a modern industrial grain harvester—cutting entire rows of wheat at once rather than plucking each grain by hand.",
    "Week 1: NumPy ndarrays and vectorization. Week 2: Pandas indexing (loc vs iloc). Week 3: Large file chunking and memory optimization.",
    "पायथन डेटा साइंस: नमपाई और पांडास (Python Data Science)",
    "नमपाई वेक्टर ऑपरेशन्स को आधुनिक फसल काटने वाली मशीन (हार्वेस्टर) की तरह समझें—जो हाथ से एक-एक दाना चुनने के बजाय पूरी पंक्ति एक बार में काट देती है।",
    "सप्ताह 1: नमपाई वेक्टर और तेज गणना। सप्ताह 2: पांडास डेटाफ्रेम (loc व iloc)। सप्ताह 3: चंकिंग और बड़ी फाइलों का मेमोरी प्रबंधन।",
    "பைதான் தரவு அறிவியல்: NumPy மற்றும் Pandas (Python Data Science)",
    "NumPy என்பது அறுவடை இயந்திரம் போன்றது—ஒவ்வொரு கதிராகப் பறிக்காமல் ஒரே நேரத்தில் முழு வயலையும் அறுவடை செய்கிறது.",
    "வாரம் 1: NumPy வெக்டார்கள். வாரம் 2: Pandas அட்டவணைகள். வாரம் 3: நினைவக மேலாண்மை.",
    "పైథాన్ డేటా సైన్స్: NumPy & Pandas (Python Data Science)",
    "NumPy ను ఆధునిక వరి కోత యంత్రంతో పోల్చవచ్చు—చేత్తో ఒక్కో గింజ కోయడానికి బదులు ఒకేసారి మొత్తం పంటను కోస్తుంది.",
    "వారం 1: NumPy అర్రేలు & వెక్టరైజేషన్. వారం 2: Pandas డేటాఫ్రేమ్స్. వారం 3: మెమరీ ఆప్టిమైజేషన్.",
    "পাইথন ডেটা সায়েন্স: NumPy এবং Pandas (Python Data Science)",
    "NumPy ভেক্টরাইজেশনকে আধুনিক ধান কাটার মেশিনের সাথে তুলনা করা যায়—হাত দিয়ে একটা একটা শিষ না কেটে যা একবারে পুরো খেত কেটে ফেলে।",
    "সপ্তাহ ১: NumPy অ্যারে ও ভেক্টরাইজেশন। সপ্তাহ ২: Pandas ইনডেক্সিং। সপ্তাহ ৩: মেমরি সাশ্রয়ী ফাইল প্রসেসিং।",
    "पायथन डेटा सायन्स: NumPy आणि Pandas (Python Data Science)",
    "NumPy म्हणजे शेतातील आधुनिक मळणी यंत्रासारखे आहे—हाताने एकेक दाणा निवडण्यापेक्षा एकाच फेरीत संपूर्ण पिकाची कापणी होते.",
    "आठवडा १: NumPy अरे आणि व्हेक्टरायझेशन. आठवडा २: Pandas डेटाफ्रेम्स. आठवडा ३: मोठ्या फाइल्सचे मेमरी व्यवस्थापन."
)

add_indic(
    "skill_02_ml_foundations",
    "Machine Learning Foundations & Supervised Models",
    "Think of model overfitting like a student who memorizes yesterday's exam answer key word-for-word—they score 100% on the old paper, but fail completely when questions change.",
    "Week 1: Linear and Logistic Regression. Week 2: Decision Trees and Overfitting vs Underfitting. Week 3: ROC-AUC and Cross-Validation.",
    "मशीन लर्निंग आधार और सुपरवाइज्ड मॉडल्स (Machine Learning)",
    "ओवरफिटिंग को उस विद्यार्थी की तरह समझें जो पुराने प्रश्नपत्र की कुंजी रट लेता है—पुराने पेपर में 100 अंक लाता है, पर नए सवाल आते ही अनुत्तीर्ण हो जाता है।",
    "सप्ताह 1: लीनियर और लॉजिस्टिक रिग्रेशन। सप्ताह 2: डिसिजन ट्री और ओवरफिटिंग रोकथाम। सप्ताह 3: ROC-AUC और क्रॉस-वैलिडेशन।",
    "இயந்திர கற்றல் அடிப்படைகள் (Machine Learning)",
    "ஓவர்ஃபிட்டிங் என்பது வினாத்தாளைப் பார்த்து மனப்பாடம் செய்யும் மாணவர் போன்றது—புதிய வினாக்கள் வரும்போது தோல்வியடைகிறார்.",
    "வாரம் 1: பின்னடைவு மாதிரிகள் (Regression). வாரம் 2: முடிவு மரங்கள் (Decision Trees). வாரம் 3: மாதிரி மதிப்பீடு.",
    "మెషిన్ లెర్నింగ్ ఫౌండేషన్స్ (Machine Learning)",
    "ఓవర్‌ఫిట్టింగ్‌ను పాత పేపర్లు బట్టీపట్టే విద్యార్థితో పోల్చవచ్చు—పాత ప్రశ్నలకు సరైన సమాధానం రాసినా కొత్త పరీక్షల్లో ఫెయిల్ అవుతాడు.",
    "వారం 1: లీనియర్ & లాజిస్టిక్ రిగ్రెషన్. వారం 2: డెసిషన్ ట్రీస్. వారం 3: క్రాస్-వాలిడేషన్ & ROC-AUC.",
    "মেশিন লার্নিং ভিত্তি ও মডেল তৈরি (Machine Learning)",
    "ওভারফিটিংকে মুখস্থ বিদ্যা করা ছাত্রের সাথে তুলনা করা যায়—যে পুরোনো প্রশ্নপত্রে পুরো নম্বর পায়, কিন্তু নতুন প্রশ্ন আসলেই থমকে যায়।",
    "সপ্তাহ ১: লিনিয়ার ও লজিস্টিক রিগ্রেশন। সপ্তাহ ২: ডিসিশন ট্রি ও ওভারফিটিং দমন। সপ্তাহ ৩: ক্রস-ভ্যালিডেশন।",
    "मशीन लर्निंगची पायाभरणी (Machine Learning)",
    "ओव्हरफिटिंग म्हणजे जुना पेपर घोकंपट्टी करणाऱ्या विद्यार्थ्यासारखे आहे—जुना पेपर आला तर पैकीच्या पैकी गुण, पण नवीन प्रश्न आला की भोपळा.",
    "आठवडा १: रिग्रेशन पद्धती. आठवडा २: डिसिजन ट्री आणि बायस-व्हेरियन्स. आठवडा ३: मॉडेल मूल्यांकन व अचूकता."
)

add_indic(
    "skill_03_deep_learning",
    "Deep Learning, CNNs & Backpropagation",
    "Think of a convolutional neural network (CNN) like an artisan gem cutter inspecting a raw diamond through multiple magnifying lenses—first edges, then facets, then overall radiance.",
    "Week 1: Multi-layer perceptrons and forward pass. Week 2: Convolutions and feature maps. Week 3: Activation functions (ReLU) and Dropout regularization.",
    "डीप लर्निंग, सीएनएन और बैकप्रॉपेगेशन (Deep Learning)",
    "सीएनएन (CNN) को एक जौहरी की तरह समझें जो हीरे की जांच करता है—पहले आवर्धक लेंस से उसके किनारे देखता है, फिर सतह और अंत में उसकी पूरी चमक पहचानता है।",
    "सप्ताह 1: मल्टी-लेयर परसेप्ट्रॉन और एक्टिवेशन। सप्ताह 2: कनवल्शन फिल्टर और फीचर मैप्स। सप्ताह 3: ड्रॉपआउट और बैकप्रॉपेगेशन।",
    "ஆழ்ந்த கற்றல் மற்றும் சிஎன்என் (Deep Learning)",
    "சிஎன்என் என்பது ஒரு வைர வியாபாரி பூதக்கண்ணாடி மூலம் வைரத்தின் விளிம்புகள் மற்றும் தரத்தை படிப்படியாக ஆராய்வது போன்றது.",
    "வாரம் 1: நியூரல் நெட்வொர்க்குகள். வாரம் 2: கன்வல்யூஷன் லேயர்கள். வாரம் 3: பேக்-ப்ரோபகேஷன் மற்றும் டிராப்அவுட்.",
    "డీప్ లెర్నింగ్ & బ్యాక్‌ప్రాపగేషన్ (Deep Learning)",
    "CNN ను రత్న వ్యాపారితో పోల్చవచ్చు—భూతద్దంతో మొదట అంచులను, తర్వాత లోపలి పొరలను, చివరికి వజ్రం అసలైన నాణ్యతను గుర్తిస్తాడు.",
    "వారం 1: న్యూరల్ నెట్‌వర్క్స్. వారం 2: కన్వల్యూషన్ లేయర్లు. వారం 3: యాక్టివేషన్ ఫంక్షన్లు & డ్రాప్‌అవుట్.",
    "ডিপ লার্নিং ও কনভলিউশনাল নেটওয়ার্ক (Deep Learning)",
    "CNN-কে একজন জহুরির সাথে তুলনা করা যায়—যিনি ম্যাগনিফাইং গ্লাস দিয়ে প্রথমে হীরার ধার, তারপর তল এবং শেষে তার সম্পূর্ণ ঔজ্জ্বল্য যাচাই করেন।",
    "সপ্তাহ ১: মাল্টি-লেয়ার পারসেপট্রন। সপ্তাহ ২: কনভলিউশন ফিল্টার ও ফিচার ম্যাপ। সপ্তাহ ৩: ব্যাকপ্রোপাগেশন।",
    "डीप लर्निंग आणि न्यूरल नेटवर्क्स (Deep Learning)",
    "CNN म्हणजे हिऱ्याची पारख करणाऱ्या रत्नपारख्यासारखे आहे—आधी तो कडा पाहतो, मग पैलू तपासतो आणि शेवटी संपूर्ण हिऱ्याचे तेज ओळखतो.",
    "आठवडा १: न्यूरल नेटवर्कचे मूळ तत्त्व. आठवडा २: कन्व्होल्युशन आणि फीचर मॅप्स. आठवडा ३: ड्रॉपआउट व बॅकप्रॉपॅगेशन."
)

# -----------------------------------------------------------------------------
# DOMAIN 6: Lifelong Learning Plans (Cognitive Habits & Micro-Learning)
# -----------------------------------------------------------------------------
add_indic(
    "life_01_micro_habit",
    "Micro-Habits & Spaced Repetition Retention",
    "Think of spaced repetition like watering a delicate flowering plant—a light splash right when the soil starts to dry sustains life far better than flooding it once a month.",
    "Week 1: Ebbinghaus forgetting curve and review schedules. Week 2: Active recall flashcards. Week 3: Habit stacking with morning routines.",
    "सूक्ष्म आदतें और स्पेस्ड पुनरावृत्ति (Micro-Habits)",
    "स्पेस्ड पुनरावृत्ति को गमले में पानी देने की तरह समझें—मिट्टी सूखने से ठीक पहले थोड़ा सा पानी देना, महीने में एक बार बाढ़ लाने से कहीं ज्यादा फलदायी होता है।",
    "सप्ताह 1: भूलने का वक्र (Forgetting Curve) और समीक्षा योजना। सप्ताह 2: सक्रिय स्मरण (Active Recall)। सप्ताह 3: आदत निर्माण और स्टैकिंग।",
    "நுண் பழக்கங்கள் மற்றும் இடைவெளி மீள்பார்வை (Micro-Habits)",
    "இடைவெளி மீள்பார்வை என்பது செடிக்கு நீர் ஊற்றுவது போன்றது—மண் காயும் முன் சிறிதளவு நீர் ஊற்றுவது மாதத்திற்கு ஒருமுறை நிரப்புவதை விட சிறந்தது.",
    "வாரம் 1: நினைவாற்றல் வளைவு. வாரம் 2: சுறுசுறுப்பான நினைவுபடுத்தல் (Active Recall). வாரம் 3: தினசரி பழக்கவழக்கங்கள்.",
    "మైక్రో అలవాట్లు & స్పేస్డ్ రిపీటీషన్ (Micro-Habits)",
    "స్పేస్డ్ రిపీటీషన్‌ను మొక్కలకు నీళ్లు పోయడంతో పోల్చవచ్చు—నేల ఎండిపోయే ముందు తగినంత నీరు పోస్తే మొక్క ఏపుగా ఎదుగుతుంది.",
    "వారం 1: మతిమరుపు గ్రాఫ్ & రివ్యూలు. వారం 2: యాక్టివ్ రీకాల్ కార్డ్స్. వారం 3: అలవాట్లను క్రమబద్ధీకరించడం.",
    "ছোট অভ্যাস ও স্পেসড পুনরাবৃত্তি (Micro-Habits)",
    "স্পেসড রিভিশনকে টবের গাছে জল দেওয়ার মতো ভাবুন—মাটি শুকানোর মুখে অল্প জল দেওয়া, মাসে একদিন অতিরিক্ত জল ঢালার চেয়ে অনেক বেশি কার্যকর।",
    "সপ্তাহ ১: ভুলে যাওয়ার কার্ভ ও রিভিশন রুটিন। সপ্তাহ ২: অ্যাক্টিভ রিকল ফ্ল্যাশকার্ড। সপ্তাহ ৩: প্রতিদিনের অভ্যাসের ধারাবাহিকতা।",
    "लहान सवयी आणि सातत्यपूर्ण पुनरावृत्ती (Micro-Habits)",
    "स्पेस्ड रिपीटिशन म्हणजे कुंडीतील रोपट्याला पाणी देण्यासारखे आहे—माती सुकण्याच्या बेतात असताना थोडे पाणी घालणे, महिन्यातून एकदा पूर आणण्यापेक्षा जास्त फायदेशीर ठरते.",
    "आठवडा १: विस्मरणाचा आलेख आणि उजळणी नियोजन. आठवडा २: ॲक्टिव्ह रिकॉल तंत्रज्ञान. आठवडा ३: दैनंदिन चांगल्या सवयींची जोडणी."
)

add_indic(
    "life_02_feynman_technique",
    "The Feynman Technique & Mental Models",
    "Think of the Feynman Technique like teaching your grandmother how a smartphone works—if you must resort to technical jargon, you don't truly understand it yourself yet.",
    "Week 1: Concept deconstruction and ELI5 language. Week 2: Identifying knowledge blindspots. Week 3: Building vivid cultural analogies.",
    "फाइनमैन तकनीक और मानसिक मॉडल (Feynman Technique)",
    "फाइनमैन तकनीक को दादी माँ को स्मार्टफोन चलाना सिखाने की तरह समझें—अगर आपको तकनीकी अंग्रेजी शब्दों का सहारा लेना पड़े, तो इसका मतलब आप खुद इसे ठीक से नहीं समझे हैं।",
    "सप्ताह 1: अवधारणा को सरलतम शब्दों में ढालना। सप्ताह 2: ज्ञान की कमजोर कड़ियों की पहचान। सप्ताह 3: वास्तविक जीवन के सटीक सादृश्य बनाना।",
    "ஃபெய்ன்மேன் கற்றல் முறை (Feynman Technique)",
    "ஃபெய்ன்மேன் முறை என்பது ஒரு சிறு குழந்தைக்குப் புரியும் வண்ணம் எளிமையாக விளக்குவது போன்றது—கடினமான சொற்கள் இல்லாமல் விளக்குவதே உண்மையான புரிதல்.",
    "வாரம் 1: எளிய விளக்கம் அளித்தல். வாரம் 2: அறியாத இடைவெளிகளைக் கண்டறிதல். வாரம் 3: கலாச்சார ஒப்புமைகளை உருவாக்குதல்.",
    "ఫేన్‌మ్యాన్ టెక్నిక్ & మెంటల్ మోడల్స్ (Feynman Technique)",
    "ఫేన్‌మ్యాన్ పద్ధతిని ఒక చిన్న పిల్లాడికి సులభంగా అర్థమయ్యేలా చెప్పడంతో పోల్చవచ్చు—క్లిష్టమైన పదాలు వాడకుండా చెప్పగలిగితేనే మనకు పూర్తిగా అర్థమైనట్లు.",
    "వారం 1: క్లిష్టమైన విషయాలను సరళీకరించడం. వారం 2: లోపాలను గుర్తించడం. వారం 3: నిత్యజీవిత ఉదాహరణలు.",
    "ফেইনম্যান পদ্ধতি ও চিন্তার মডেল (Feynman Technique)",
    "ফেইনম্যান পদ্ধতিকে ঠাকুরমাকে কোনো নতুন প্রযুক্তি বোঝানোর মতো ভাবতে পারেন—যদি জটিল শব্দের সাহায্য নিতে হয়, তবে বুঝতে হবে আপনার নিজেরই বোঝায় ঘাটতি আছে।",
    "সপ্তাহ ১: সবচেয়ে সহজ ভাষায় ধারণা প্রকাশ। সপ্তাহ ২: অজ্ঞতার ফাঁকফোকর চিহ্নিতকরণ। সপ্তাহ ৩: বাস্তবিক উপমার মেলবন্ধন।",
    "फाइनमन तंत्र आणि वैचारिक मॉडेल (Feynman Technique)",
    "फाइनमन तंत्र म्हणजे आपल्या आजीला स्मार्टफोन कसा चालवायचा हे सोप्या भाषेत समजावून सांगण्यासारखे आहे—जर तुम्हाला अवघड तांत्रिक शब्द वापरावे लागले, तर तुम्हालाच ते नीट समजलेले नाही.",
    "आठवडा १: विषयाचे सोप्या भाषेत वर्गीकरण. आठवडा २: स्वतःमधील उणिवा शोधणे. आठवडा ३: दैनंदिन जीवनातील समर्पक दाखले."
)

add_indic(
    "life_03_deep_focus",
    "Deep Focus, Attention Hygiene & Pomodoro Flow",
    "Think of attention residue like wet mud on your shoes—every time you dash into social media for 'just 10 seconds', you drag muddy footprints back into your primary workspace for the next 20 minutes.",
    "Week 1: Attention residue and cognitive friction. Week 2: 25-minute Pomodoro sprints and detachment breaks. Week 3: Dopamine fasting and deep work ritual.",
    "गहन एकाग्रता और पोमोडोरो प्रवाह (Deep Focus)",
    "अटेंशन रेसिड्यू (ध्यान के भटकाव) को जूतों पर लगी गीली मिट्टी की तरह समझें—हर बार जब आप 'सिर्फ 10 सेकंड' के लिए फोन उठाते हैं, तो अगले 20 मिनट तक आपके काम की मेज पर कीचड़ के निशान छूटते रहते हैं।",
    "सप्ताह 1: ध्यान भटकाव और कार्य स्विचिंग के खतरे। सप्ताह 2: 25 मिनट के पोमोडोरो स्प्रिंट्स और विश्राम। सप्ताह 3: डोपामाइन नियंत्रण और डीप वर्क वातावरण।",
    "ஆழ்ந்த கவனம் மற்றும் போமோடோரோ ஓட்டம் (Deep Focus)",
    "கவனச் சிதறல் என்பது காலில் உள்ள சேறு போன்றது—ஒருமுறை கவனத்தைத் திருப்பினால் அடுத்த 20 நிமிடங்களுக்கு அதன் தாக்கம் நீடிக்கும்.",
    "வாரம் 1: கவனப் பிடிப்பு தத்துவம். வாரம் 2: 25 நிமிட போமோடோரோ சுழற்சிகள். வாரம் 3: ஆழ்ந்த பணிப் பழக்கங்கள்.",
    "డీప్ ఫోకస్ & పోమోడోరో పద్ధతి (Deep Focus)",
    "ధ్యాస చెదిరిపోవడాన్ని బూట్లకు అంటిన బురదతో పోల్చవచ్చు—కేవలం పది సెకన్ల పాటు ఫోన్ చూసినా, ఆ తర్వాతి 20 నిమిషాల ఏకాగ్రత దెబ్బతింటుంది.",
    "వారం 1: మైండ్ డైవర్షన్ నియంత్రణ. వారం 2: 25 నిమిషాల పోమోడోరో స్ప్రింట్స్. వారం 3: డీప్ వర్క్ అలవాట్లు.",
    "গভীর মনোযোগ ও পোমোডোরো ফ্লো (Deep Focus)",
    "মনোযোগের রেশকে জুতোর কাদামাটির মতো ভাবুন—মাত্র দশ সেকেন্ডের জন্য ফোনে চোখ বোলালেও তার অস্বস্তি পরের কুড়ি মিনিট কাজের ক্ষতি করে।",
    "সপ্তাহ ১: মনোযোগ বিচ্যুতি ও কাজের ব্যাঘাত। সপ্তাহ ২: ২৫ মিনিটের পোমোডোরো চক্র। সপ্তাহ ৩: নিরবচ্ছিন্ন কাজের পরিবেশ তৈরি।",
    "खोल एकाग्रता आणि पोमोडोरो पद्धत (Deep Focus)",
    "एकाग्रतेचा भंग म्हणजे चिखलातील पावलांसारखा आहे—अवघ्या १० सेकंदांसाठी मोबाईल पाहिला तरी पुढची २० मिनिटे बुद्धी पूर्ववत एकाग्र होत नाही.",
    "आठवडा १: लक्ष विचलित होण्याचे तोटे. आठवडा २: २५ मिनिटांचे पोमोडोरो सत्र आणि विश्रांती. आठवडा ३: अखंड अभ्यासाची शिस्त."
)

cur.executemany("INSERT INTO indic_content_fts VALUES (?, ?, ?, ?, ?, ?);", indic_records)

conn.commit()

# Sanity checks
cur.execute("SELECT COUNT(*) FROM skills_catalog;")
total_skills = cur.fetchone()[0]

cur.execute("SELECT COUNT(*) FROM indic_content_fts;")
total_indic = cur.fetchone()[0]

cur.execute("SELECT COUNT(DISTINCT lang_code) FROM indic_content_fts;")
total_langs = cur.fetchone()[0]

cur.execute("SELECT lang_code, COUNT(*) FROM indic_content_fts GROUP BY lang_code;")
per_lang = cur.fetchall()

# Answer distribution sanity check
cur.execute("SELECT diagnostic_quiz_json FROM skills_catalog;")
all_quizzes = cur.fetchall()
ans_counts = {0: 0, 1: 0, 2: 0, 3: 0}
total_questions = 0
for (q_json,) in all_quizzes:
    for q in json.loads(q_json):
        ans_counts[q["ans"]] += 1
        total_questions += 1

print(f"Database build complete!")
print(f"Total skills in catalog: {total_skills}")
print(f"Total localized content rows: {total_indic}")
print(f"Distinct languages: {total_langs}")
print(f"Rows per language: {per_lang}")
print(f"Total quiz questions: {total_questions}")
print(f"Answer distribution across indices (0=A, 1=B, 2=C, 3=D): {ans_counts}")

conn.close()

# Copy to Android assets
for dst_path in target_paths[1:]:
    os.makedirs(os.path.dirname(dst_path), exist_ok=True)
    shutil.copy(LOCAL_DB_PATH, dst_path)
    asset_size = os.path.getsize(dst_path)
    print(f"Synchronized to Android assets: {dst_path} ({asset_size} bytes)")
